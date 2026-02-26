package com.back.global.security

import com.back.global.rateLimit.Bucket4jRateLimiter
import com.back.global.rateLimit.RateLimitPolicy
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@ConditionalOnProperty(
    name = ["ratelimit.enabled"],
    havingValue = "true",
    matchIfMissing = true
)class ApiRateLimitFilter(
    private val rateLimiter: Bucket4jRateLimiter
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val securityUser = SecurityContextHolder.getContext().authentication
            ?.takeIf { it.isAuthenticated }
            ?.principal as? SecurityUser

        securityUser?.let { user ->
            val key = "api:${user.id}"
            val policy = resolvePolicy(request.requestURI)

            if (!rateLimiter.tryConsume(key, policy)) {
                log.warn(
                    "Rate limit exceeded - ip: {}, uri: {}, method: {}",
                    request.getRemoteAddr(),
                    request.getRequestURI(),
                    request.getMethod()
                )

                response.setStatus(429)
                response.setContentType("application/json;charset=UTF-8")
                response.getWriter().write(
                    """
                {
                  "code": "429-1",
                  "message": "요청이 너무 많습니다."
                }
                
                """.trimIndent()
                )
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolvePolicy(path: String): RateLimitPolicy =
        when{
            path.startsWith("/api/orders") -> RateLimitPolicy.API_HEAVY
            else -> RateLimitPolicy.API_DEFAULT
        }
}
