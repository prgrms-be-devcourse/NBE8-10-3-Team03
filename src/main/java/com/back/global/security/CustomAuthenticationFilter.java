package com.back.global.security;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.MemberStatus;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.standard.util.Ut;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationFilter extends OncePerRequestFilter {
    private final MemberService memberService;
    private final Rq rq;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        logger.debug("Processing request for " + request.getRequestURI());

        try {
            work(request, response, filterChain);
        } catch (ServiceException e) {
            RsData<Void> rsData = e.getRsData();
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(rsData.statusCode());
            response.getWriter().write(
                    Ut.json.toString(rsData)
            );
        } catch (Exception e) {
            throw e;
        }
    }

    private void work(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // API 요청이 아니라면 패스
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 인증, 인가가 필요없는 API 요청이라면 패스
        if (List.of("/api/v1/members/login", "/api/v1/members/logout", "/api/v1/members/join").contains(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey;
        String accessToken;

        // 토큰 추출 로직 (헤더 -> 쿠키)
        String headerAuthorization = rq.getHeader("Authorization", "");

        // Authorization 헤더가 있는 경우
        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer "))
                throw new ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.");

            String[] headerAuthorizationBits = headerAuthorization.split(" ", 3);

            apiKey = headerAuthorizationBits[1];
            accessToken = headerAuthorizationBits.length == 3 ? headerAuthorizationBits[2] : "";

            // 헤더에 apiKey나 accessToken만 넣는 경우
            if (headerAuthorizationBits.length == 2) {
                if (headerAuthorizationBits[1].split("\\.").length == 3) {
                    accessToken = headerAuthorizationBits[1];
                    apiKey = "";
                }
            }

        } else {
            // 헤더가 없으면 쿠키에서 조회
            apiKey = rq.getCookieValue("apiKey", "");
            accessToken = rq.getCookieValue("accessToken", "");
        }

        logger.debug("apiKey : " + apiKey);
        logger.debug("accessToken : " + accessToken);

        boolean isApiKeyExists = !apiKey.isBlank();
        boolean isAccessTokenExists = !accessToken.isBlank();

        // apiKey(리프레시 토큰) + 액세스 토큰 둘 다 없으면 패스
        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response);
            return;
        }

        Member member = null;
        boolean isAccessTokenValid = false;

        // 액세스 토큰 검증
        if (isAccessTokenExists) {
            Map<String, Object> payload = memberService.payload(accessToken);

            if (payload != null) {
                int id = (int) payload.get("id");
                String username = (String) payload.get("username");
                String name = (String) payload.get("name");
                String role = (String) payload.get("role");

                member = memberService.findById(id).get();

                isAccessTokenValid = true;
            }
        }

        // 액세스 토큰이 없거나 실패하면 apiKey로 사용자 인증
        if (member == null) {
            member = memberService
                    .findByApiKey(apiKey)
                    .orElseThrow(() -> new ServiceException("401-3", "API 키가 유효하지 않습니다."));
        }

        // 액세스 토큰은 있는데 기간이 만료되었다면(유효하지 않다면) 재발급
        if (isAccessTokenExists && !isAccessTokenValid) {
            // 액세스 토큰 생성
            String actorAccessToken = memberService.genAccessToken(member);

            // 액세스 토큰을 쿠키와 헤더에 저장
            rq.setCookie("accessToken", actorAccessToken);
            rq.setHeader("Authorization", actorAccessToken);
        }

        // 영구 정지 회원 JWT 차단
        if (member.getStatus() == MemberStatus.BANNED) {
            throw new ServiceException("403-4", "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.");
        }

        // 탈퇴 회원 JWT 차단
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ServiceException("403-5", "탈퇴한 계정입니다.");
        }

        // Spring Security 인증 객체 생성 ⭐
        UserDetails user = new SecurityUser(
                member.getId(),
                member.getUsername(),
                "",
                member.getNickname(),
                member.getRole(),
                member.getAuthorities()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                user.getPassword(),
                user.getAuthorities()
        );

        // 인증 완료 처리
        // 이 시점 이후부터는 시큐리티가 이 요청을 인증된 사용자의 요청이다.
        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        // 다음 필터로 넘김
        filterChain.doFilter(request, response);
    }
}