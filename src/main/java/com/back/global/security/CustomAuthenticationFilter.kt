package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.enums.Role.Companion.from
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val memberService: MemberService,
    private val rq: Rq
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("Processing request for ${request.requestURI}")

        try {
            work(request, response, filterChain)
        } catch (e: ServiceException) {
            val rsData = e.rsData
            response.contentType = "application/json;charset=UTF-8"
            response.status = rsData.statusCode
            response.writer.write(Ut.json.toString(rsData))
        }
    }

    private fun work(request: HttpServletRequest, response: HttpServletResponse?, filterChain: FilterChain) {
        // API 요청이 아니라면 패스
        if (!request.requestURI.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        // 인증, 인가가 필요없는 API 요청이라면 패스
        val publicEndpoints = listOf("/api/v1/members/login", "/api/v1/members/logout", "/api/v1/members/join")
        if (request.requestURI in publicEndpoints) {
            filterChain.doFilter(request, response)
            return
        }

        // 토큰 추출 로직 (헤더 -> 쿠키)
        val headerAuthorization = rq.getHeader("Authorization", "")
        val (apiKey, accessToken) = parseTokens(headerAuthorization)

        logger.debug("apiKey: $apiKey")
        logger.debug("accessToken: $accessToken")

        val isApiKeyExists = !apiKey.isBlank()
        val isAccessTokenExists = !accessToken.isBlank()

        // apiKey(리프레시 토큰) + 액세스 토큰 둘 다 없으면 패스
        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response)
            return
        }

        val accessTokenMember = if (isAccessTokenExists) resolveMemberFromAccessToken(accessToken) else null
        val member = accessTokenMember
            ?: memberService.findByApiKey(apiKey).orElseThrow { ServiceException("401-3", "API 키가 유효하지 않습니다.") }
        val isAccessTokenValid = accessTokenMember != null

        // 액세스 토큰은 있는데 기간이 만료되었다면(유효하지 않다면) 재발급
        if (isAccessTokenExists && !isAccessTokenValid) {
            // 액세스 토큰 생성
            val actorAccessToken = memberService.genAccessToken(member)

            // 액세스 토큰을 쿠키와 헤더에 저장
            rq.setCookie("accessToken", actorAccessToken)
            rq.setHeader("Authorization", actorAccessToken)
        }

        when (member.status) {
            // 영구 정지 회원 JWT 차단
            MemberStatus.BANNED -> throw ServiceException("403-4", "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.")
            // 탈퇴 회원 JWT 차단
            MemberStatus.WITHDRAWN -> throw ServiceException("403-5", "탈퇴한 계정입니다.")
            else -> Unit
        }

        // Spring Security 인증 객체 생성 ⭐
        val user: UserDetails = SecurityUser(
            member.id,
            member.username,
            "",
            member.nickname,
            member.role,
            member.authorities
        )

        val authentication: Authentication = UsernamePasswordAuthenticationToken(
            user,
            user.password,
            user.authorities
        )

        // 인증 완료 처리
        // 이 시점 이후부터는 시큐리티가 이 요청을 인증된 사용자의 요청이다.
        SecurityContextHolder
            .getContext()
            .authentication = authentication

        // 다음 필터로 넘김
        filterChain.doFilter(request, response)
    }

    private fun parseTokens(headerAuthorization: String): Pair<String, String> {
        if (headerAuthorization.isBlank()) {
            return rq.getCookieValue("apiKey", "") to rq.getCookieValue("accessToken", "")
        }

        if (!headerAuthorization.startsWith("Bearer ")) {
            throw ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.")
        }

        val parts = headerAuthorization.split(" ", limit = 3)
        val bearerValue = parts.getOrNull(1)
            ?: throw ServiceException("401-2", "Authorization 헤더가 Bearer 형식이 아닙니다.")

        // "Bearer <jwt>" 형태는 accessToken 전용, 그 외는 "Bearer <apiKey> <accessToken?>"로 해석한다.
        return if (parts.size == 2 && bearerValue.split(".").size == 3) {
            "" to bearerValue
        } else {
            bearerValue to parts.getOrElse(2) { "" }
        }
    }

    private fun resolveMemberFromAccessToken(accessToken: String): Member? =
        memberService.payload(accessToken)?.let { payload ->
            Member(
                payload["id"] as Int,
                payload["username"] as String,
                payload["name"] as String,
                from(payload["role"] as? String),
            )
        }
}
