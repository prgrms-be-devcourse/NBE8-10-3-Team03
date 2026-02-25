package com.back.global.security

import com.back.domain.chat.chat.service.port.ChatMemberPort
import com.back.domain.chat.chat.service.port.ChatRoomAccessPort
import com.back.global.exception.ServiceException
import com.back.global.security.port.JwtPayloadDecoderPort
import com.back.global.security.port.WsAuthenticationFactoryPort
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher

/**
 * STOMP 메시지 핸들러 (인터셉터)
 * 웹소켓 연결, 구독 요청 시 인증 및 인가(권한 체크)를 수행합니다.
 */
@Component
class StompHandler(
    private val jwtPayloadDecoderPort: JwtPayloadDecoderPort,
    private val chatRoomAccessPort: ChatRoomAccessPort,
    private val chatMemberPort: ChatMemberPort,
    private val wsAuthenticationFactoryPort: WsAuthenticationFactoryPort,
) : ChannelInterceptor {
    private val pathMatcher = AntPathMatcher()

    /**
     * 메시지가 채널로 전송되기 직전에 호출됩니다.
     * CONNECT(연결), SUBSCRIBE(구독) 명령어를 낚아채서 보안 검사를 수행합니다.
     */
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        // 메시지 헤더에 접근하기 위한 Accessor 생성
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message

        when (accessor.command) {
            StompCommand.CONNECT -> handleConnect(accessor) // 처음 연결할 때 토큰 검증
            StompCommand.SUBSCRIBE -> handleSubscribe(accessor) // 특정 방/알림 구독할 때 권한 검증
            else -> Unit
        }

        // 인증 정보를 SecurityContext에 전파하여 @AuthenticationPrincipal 등 사용 가능하게 함
        propagateSecurityContext(accessor)
        return message
    }

    /**
     * 메시지 전송이 완료된 후 호출됩니다.
     * 스레드 로컬에 저장된 SecurityContext를 비워 메모리 누수 및 정보 오염을 방지합니다.
     */
    override fun afterSendCompletion(message: Message<*>, channel: MessageChannel, sent: Boolean, ex: Exception?) {
        SecurityContextHolder.clearContext()
    }

    /**
     * [연결 처리]
     * 웹소켓 handshake 시점의 인증 정보를 확인하거나, STOMP 헤더의 'token'을 검증합니다.
     */
    private fun handleConnect(accessor: StompHeaderAccessor) {
        // 방법 1: HTTP Handshake 과정에서 이미 인증된 정보가 있는지 확인 (HandshakeInterceptor 연동)
        val handshakeAuth = accessor.sessionAttributes
            ?.get(HttpHandshakeInterceptor.WS_AUTHENTICATION_KEY) as? Authentication

        handshakeAuth?.let { auth ->
            accessor.user = auth
            val user = auth.principal as? SecurityUser
            log.info("STOMP Connected (핸드셰이크 인증): {}", user?.username)
            return
        }

        // 방법 2: STOMP nativeHeader에 담긴 Bearer 토큰 직접 검증 (모바일 또는 수동 연결 시)
        val bearerToken = resolveBearerToken(accessor)
            ?: throw ServiceException("401-1", "인증 토큰이 필요합니다.")

        // JWT 토큰 파싱 및 유저 정보 추출
        val payload = runCatching { jwtPayloadDecoderPort.decode(bearerToken) }
            .getOrElse {
                log.error("STOMP Connect Auth Error: {}", it.message)
                throw ServiceException("401-1", "유효하지 않은 토큰입니다.")
            } ?: throw ServiceException("401-1", "유효하지 않은 토큰입니다.")

        // 인증 객체 생성 및 세션에 저장
        val auth = wsAuthenticationFactoryPort.fromPayload(payload)
        accessor.user = auth
        val user = auth.principal as SecurityUser

        log.info("STOMP Connected (토큰 인증): {}", user.username)
    }

    /**
     * [구독 권한 처리]
     * 유저가 특정 채팅방이나 알림 채널을 볼 권한이 있는지 꼼꼼하게 검사합니다.
     */
    private fun handleSubscribe(accessor: StompHeaderAccessor) {
        val destination = accessor.destination ?: return

        // 1. 채팅방 메시지 구독 (/sub/v1/chat/room/{roomId}/**)
        if (pathMatcher.match(CHAT_ROOM_SUBSCRIBE_PATTERN, destination)) {
            val user = requireSecurityUser(accessor)
            val roomId = extractRoomId(destination)

            // 채팅방 존재 여부 확인
            val room = chatRoomAccessPort.getActiveRoomOrThrow(roomId)
            val member = chatMemberPort.getMemberOrThrow(user.id)

            // 인가 검사: 내가 이 방의 판매자인가? 혹은 구매자인가?
            val authorized = room.sellerApiKey == member.apiKey || room.buyerApiKey == member.apiKey
            if (!authorized) {
                log.warn("Access Denied: User {} -> Room {}", user.id, roomId)
                throw ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.")
            }

            log.info("STOMP Subscribed: User {} to Room {}", user.username, roomId)
            return
        }

        // 개인 알림 구독 (/sub/user/{userId}/notification)
        if (pathMatcher.match(PERSONAL_NOTIFICATION_PATTERN, destination)) {
            val user = requireSecurityUser(accessor)
            val pathParts = splitPath(destination)
            if (pathParts.size < 4) {
                throw ServiceException("400-1", "잘못된 구독 경로입니다.")
            }

            // 인가 검사: 구독하려는 목적지 ID와 현재 로그인한 내 ID가 일치하는가?
            val targetUserId = pathParts[2]
            if (user.id.toString() != targetUserId) {
                log.warn(
                    "Access Denied: User {} tried to subscribe to User {}'s notification",
                    user.id,
                    targetUserId,
                )
                throw ServiceException("403-1", "개인 알림 구독 권한이 없습니다.")
            }

            log.info("STOMP Subscribed: User {} to personal notification", user.username)
        }
    }

    /**
     * Accessor에 저장된 인증 정보를 SecurityContextHolder로 복사합니다.
     */
    private fun propagateSecurityContext(accessor: StompHeaderAccessor) {
        val auth = accessor.user as? Authentication ?: return
        SecurityContextHolder.getContext().authentication = auth
    }

    /**
     * 인증된 유저 정보를 가져오며, 없을 경우 즉시 예외를 발생시킵니다.
     */
    private fun requireSecurityUser(accessor: StompHeaderAccessor): SecurityUser {
        val auth = accessor.user as? Authentication
        val principal = auth?.principal as? SecurityUser
        return principal ?: throw ServiceException("401-1", "로그인이 필요합니다.")
    }

    /**
     * 구독 경로에서 채팅방의 고유 ID(roomId)를 추출합니다.
     * /read 가 붙은 경로(읽음 알림용)도 처리할 수 있도록 설계되었습니다.
     */
    private fun extractRoomId(destination: String): String {
        val parts = splitPath(destination)
        if (parts.isEmpty()) {
            throw ServiceException("400-1", "잘못된 구독 경로입니다.")
        }

        return if (destination.endsWith(READ_SUFFIX)) {
            // /sub/v1/chat/room/{roomId}/read 인 경우 뒤에서 두 번째가 ID
            parts.getOrNull(parts.size - 2) ?: throw ServiceException("400-1", "잘못된 구독 경로입니다.")
        } else {
            // /sub/v1/chat/room/{roomId} 인 경우 마지막이 ID
            parts.last()
        }
    }

    /** 경로를 슬래시(/) 기준으로 쪼개고 빈 값을 제거하여 리스트로 반환 */
    private fun splitPath(path: String): List<String> =
        path.split("/").filter(String::isNotBlank)

    private fun resolveBearerToken(accessor: StompHeaderAccessor): String? {
        val tokenHeader = accessor.getFirstNativeHeader(TOKEN_HEADER_NAME)?.trim()
        if (!tokenHeader.isNullOrBlank()) {
            return tokenHeader.removeBearerPrefixOrNull()
        }

        val authorizationHeader = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER_NAME)?.trim()
        return authorizationHeader?.removeBearerPrefixOrNull()
    }

    private fun String.removeBearerPrefixOrNull(): String? {
        if (!startsWith(BEARER_PREFIX)) return null
        return removePrefix(BEARER_PREFIX).takeIf(String::isNotBlank)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        private const val READ_SUFFIX = "/read"
        private const val CHAT_ROOM_SUBSCRIBE_PATTERN = "/sub/v1/chat/room/**"
        private const val PERSONAL_NOTIFICATION_PATTERN = "/sub/user/*/notification"
        private const val TOKEN_HEADER_NAME = "token"
        private const val AUTHORIZATION_HEADER_NAME = "Authorization"
        private val log = LoggerFactory.getLogger(StompHandler::class.java)
    }
}
