package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import com.back.global.rq.Rq;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final MemberService memberService;
    private final Rq rq;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Member actor = rq.getActorFromDb();

        String accessToken = memberService.genAccessToken(actor);

        rq.setHeader("Authorization", "Bearer " + actor.getApiKey() + " " + accessToken);
        rq.setCookie("apiKey", actor.getApiKey());
        rq.setCookie("accessToken", accessToken);

        // ✅ 기본 리다이렉트 URL
        String redirectUrl = "/";

        // ✅ state 파라미터 확인
        String stateParam = request.getParameter("state");

        if (stateParam != null) {
            // 1️⃣ Base64 URL-safe 디코딩
            String decodedStateParam = new String(Base64.getUrlDecoder().decode(stateParam), StandardCharsets.UTF_8);

            // 2️⃣ '#' 앞은 redirectUrl, 뒤는 originState
            redirectUrl = decodedStateParam.split("#", 2)[0];
        }

        // ✅ 최종 리다이렉트
        rq.sendRedirect(redirectUrl);
    }
}
