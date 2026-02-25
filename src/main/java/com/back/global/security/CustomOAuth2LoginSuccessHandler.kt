package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.rq.Rq
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.io.IOException
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
        authentication: Authentication
    ) {
        val actor = rq.actorFromDb

        val accessToken = memberService.genAccessToken(actor)

        rq.setHeader("Authorization", "Bearer " + actor.apiKey + " " + accessToken)
        rq.setCookie("apiKey", actor.apiKey)
        rq.setCookie("accessToken", accessToken)

        // ✅ 기본 리다이렉트 URL
        var redirectUrl: String = "/"

        // ✅ state 파라미터 확인
        val stateParam = request.getParameter("state")

        if (stateParam != null) {
            // 1️⃣ Base64 URL-safe 디코딩
            val decodedStateParam = String(Base64.getUrlDecoder().decode(stateParam), StandardCharsets.UTF_8)

            // 2️⃣ '#' 앞은 redirectUrl, 뒤는 originState
            redirectUrl = decodedStateParam.split("#".toRegex(), limit = 2).toTypedArray()[0]
        }

        // ✅ 최종 리다이렉트
        rq.sendRedirect(redirectUrl)
    }
}
