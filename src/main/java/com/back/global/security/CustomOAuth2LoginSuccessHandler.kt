package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.*

@Component
class CustomOAuth2LoginSuccessHandler(
    private val memberService: MemberService,
    private val rq: Rq
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val actorId = rq.actor.id
        val actor = memberService.findById(actorId)
            ?: throw  IllegalStateException("인증된 사용자를 DB에서 찾을 수 없습니다. id=$actorId")

        val accessToken = memberService.genAccessToken(actor)

        rq.setHeader("Authorization", "Bearer ${actor.apiKey} $accessToken")
        rq.setCookie("apiKey", actor.apiKey)
        rq.setCookie("accessToken", accessToken)

        // ✅ state 파라미터 확인
        val stateParam = request.getParameter("state")
        // state는 "redirectUrl#nonce"를 Base64 URL-safe로 인코딩한 값이므로 '#' 앞부분만 redirect URL로 사용한다.
        val redirectUrl = stateParam
            ?.let { String(Base64.getUrlDecoder().decode(it), StandardCharsets.UTF_8) }
            ?.substringBefore("#")
            ?: "/"

        // ✅ 최종 리다이렉트
        rq.sendRedirect(redirectUrl)
    }
}
