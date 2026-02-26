package com.back.global.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class CustomOAuth2AuthorizationRequestResolver(
    private val clientRegistrationRepository: ClientRegistrationRepository
) : OAuth2AuthorizationRequestResolver {

    private fun createDefaultResolver(): DefaultOAuth2AuthorizationRequestResolver {
        // ✅ Spring Security 기본 Authorization URI 사용
        return DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
        )
    }

    override fun resolve(request: HttpServletRequest): OAuth2AuthorizationRequest? {
        val req = createDefaultResolver().resolve(request)
        return customizeState(req, request)
    }

    override fun resolve(request: HttpServletRequest, clientRegistrationId: String?): OAuth2AuthorizationRequest? {
        val req = createDefaultResolver().resolve(request, clientRegistrationId)
        return customizeState(req, request)
    }

    private fun customizeState(
        req: OAuth2AuthorizationRequest?,
        request: HttpServletRequest
    ): OAuth2AuthorizationRequest? {
        req ?: return null

        // ✅ 요청 파라미터에서 redirectUrl 가져오기
        val redirectUrl = request.getParameter("redirectUrl") ?: "/"

        // ✅ CSRF 방지용 nonce 추가
        val originState = UUID.randomUUID().toString()

        // ✅ redirectUrl#originState 결합
        val rawState = "$redirectUrl#$originState"

        // ✅ Base64 URL-safe 인코딩
        val encodedState = Base64.getUrlEncoder().encodeToString(rawState.toByteArray(StandardCharsets.UTF_8))

        return OAuth2AuthorizationRequest.from(req)
            .state(encodedState) // ✅ state 교체
            .build()
    }
}
