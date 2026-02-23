package com.back.global.security;

import com.back.global.rateLimit.Bucket4jRateLimiter;
import com.back.global.rateLimit.RateLimitPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final Bucket4jRateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof SecurityUser securityUser) {
            int userId = securityUser.getId();

            String key = "api:" + userId;
            RateLimitPolicy policy = resolvePolicy(request.getRequestURI());

            if (!rateLimiter.tryConsume(key, policy)) {
                log.warn("Rate limit exceeded - ip: {}, uri: {}, method: {}",
                        request.getRemoteAddr(),
                        request.getRequestURI(),
                        request.getMethod()
                );

                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("""
                {
                  "code": "429-1",
                  "message": "요청이 너무 많습니다."
                }
                """);
                return;
            }

        }

        filterChain.doFilter(request, response);
    }

    private RateLimitPolicy resolvePolicy(String path) {
        if (path.startsWith("/api/orders")) {
            return RateLimitPolicy.API_HEAVY;
        }
        return RateLimitPolicy.API_DEFAULT;
    }
}
