package com.back.global.security;

import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.Map;

/**
 * WebSocket HTTP 핸드셰이크 단계에서 브라우저가 보낸 쿠키(JWT)를 가로채어
 * 인증 정보를 WebSocket 세션 속성에 저장하는 인터셉터.
 *
 * 이후 StompHandler(ChannelInterceptor)에서 이 정보를 꺼내어
 * SecurityContextHolder에 설정하므로, WebSocket 스코프에서도
 * Spring Security 인증 정보를 사용할 수 있게 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

    public static final String WS_AUTHENTICATION_KEY = "WS_AUTHENTICATION";

    private final MemberService memberService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 쿠키에서 accessToken 추출
            String accessToken = getCookieValue(httpRequest, "accessToken");

            if (accessToken != null && !accessToken.isBlank()) {
                try {
                    Map<String, Object> payload = memberService.payload(accessToken);
                    if (payload != null) {
                        SecurityUser user = createSecurityUser(payload);
                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                user, null, user.getAuthorities());

                        // WebSocket 세션 속성에 인증 정보 저장
                        attributes.put(WS_AUTHENTICATION_KEY, auth);
                        log.info("WebSocket 핸드셰이크 인증 성공: {}", user.getUsername());
                    }
                } catch (Exception e) {
                    log.warn("WebSocket 핸드셰이크 인증 실패: {}", e.getMessage());
                }
            }
        }

        // 인증 실패해도 연결은 허용 (CONNECT 단계에서 헤더 토큰으로 재검증 가능)
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 후처리 불필요
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private SecurityUser createSecurityUser(Map<String, Object> payload) {
        int id = (int) payload.get("id");
        String username = (String) payload.get("username");
        String name = (String) payload.get("name");
        Role role = Role.from((String) payload.get("role"));

        return new SecurityUser(
                id, username, "", name, role,
                Collections.singletonList(new SimpleGrantedAuthority(role.name()))
        );
    }
}