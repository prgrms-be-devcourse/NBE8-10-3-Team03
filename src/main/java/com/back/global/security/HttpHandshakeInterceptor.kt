package com.back.global.security

import com.back.domain.member.member.enums.Role
import com.back.domain.member.member.service.MemberService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import org.slf4j.LoggerFactory

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
) : HandshakeInterceptor {

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Boolean {
        val servletRequest = (request as? ServletServerHttpRequest)?.servletRequest
        val accessToken = servletRequest?.let { getCookieValue(it, "accessToken") }

        if (!accessToken.isNullOrBlank()) {
            runCatching { memberService.payload(accessToken) }
                .mapCatching { it ?: throw RuntimeException("Unauthorized") }
                .map(::createSecurityUser)
                .onSuccess { user ->
                    val auth: Authentication = UsernamePasswordAuthenticationToken(user, null, user.authorities)
                    attributes[WS_AUTHENTICATION_KEY] = auth
                    log.info("WebSocket 핸드셰이크 인증 성공: {}", user.username)
                }
                .onFailure { e ->
                    log.warn("WebSocket 핸드셰이크 인증 실패: {}", e.message)
                }
        }

        // 인증 실패해도 연결은 허용 (CONNECT 단계에서 헤더 토큰으로 재검증 가능)
        return true
    }

    override fun afterHandshake(request: ServerHttpRequest, response: ServerHttpResponse, wsHandler: WebSocketHandler, exception: Exception?) = Unit

    private fun getCookieValue(request: HttpServletRequest, name: String): String? =
        request.cookies?.firstOrNull { it.name == name }?.value

    private fun createSecurityUser(payload: Map<String, Any>): SecurityUser {
        val id = (payload["id"] as Number).toInt()
        val username = payload["username"] as String
        val name = payload["name"] as String
        val role = Role.from(payload["role"] as String)

        return SecurityUser(
            id,
            username,
            "",
            name,
            role,
            listOf(SimpleGrantedAuthority(role.name)),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(HttpHandshakeInterceptor::class.java)
        const val WS_AUTHENTICATION_KEY: String = "WS_AUTHENTICATION"
    }
}
