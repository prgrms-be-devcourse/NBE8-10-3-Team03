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

        rq.sendRedirect("http://localhost:8080/api/v1/members");
    }
}
