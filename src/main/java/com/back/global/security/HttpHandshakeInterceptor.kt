package com.back.global.security

import com.back.domain.member.member.service.MemberService
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

/**
 * WebSocket HTTP 핸드셰이크 단계에서 브라우저가 보낸 쿠키(JWT)를 가로채어
 * 인증 정보를 WebSocket 세션 속성에 저장하는 인터셉터.
 *
 * 이후 StompHandler(ChannelInterceptor)에서 이 정보를 꺼내어
 * SecurityContextHolder에 설정하므로, WebSocket 스코프에서도
 * Spring Security 인증 정보를 사용할 수 있게 된다.
 */
@Component
class HttpHandshakeInterceptor(
    private val memberService: MemberService,
    private val webSocketAuthSupport: WebSocketAuthSupport,
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest
        val accessToken = servletRequest?.let { getCookieValue(it, ACCESS_TOKEN_COOKIE_NAME) }

        if (accessToken.isNullOrBlank()) {
            return true
        }

        runCatching { memberService.payload(accessToken) }
            .mapNotNullPayload()
            .map(webSocketAuthSupport::toAuthentication)
            .onSuccess { auth ->
                attributes[WS_AUTHENTICATION_KEY] = auth
                val user = auth.principal as? SecurityUser
                log.info("WebSocket 핸드셰이크 인증 성공: {}", user?.username)
            }
            .onFailure { e ->
                log.warn("WebSocket 핸드셰이크 인증 실패: {}", e.message)
            }

        // 인증 실패해도 연결은 허용 (CONNECT 단계에서 헤더 토큰으로 재검증 가능)
        return true
    }

    override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, exception: Exception?) = Unit

    private fun getCookieValue(request: HttpServletRequest, name: String): String? =
        request.cookies?.firstOrNull { it.name == name }?.value

    private fun Result<Map<String, Any>?>.mapNotNullPayload(): Result<Map<String, Any>> =
        mapCatching { payload -> payload ?: error("JWT payload is null.") }

    companion object {
        private val log = LoggerFactory.getLogger(HttpHandshakeInterceptor::class.java)
        private const val ACCESS_TOKEN_COOKIE_NAME = "accessToken"
        const val WS_AUTHENTICATION_KEY: String = "WS_AUTHENTICATION"
    }
}
