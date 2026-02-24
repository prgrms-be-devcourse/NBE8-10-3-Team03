package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.enums.Role.Companion.from
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException
import java.util.function.Supplier

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
            e.rsData.let { rsData ->
                response.contentType = "application/json;charset=UTF-8"
                response.status = rsData.statusCode
                response.writer.write(Ut.json.toString(rsData))
            }
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

        var apiKey: String
        var accessToken: String

        // 토큰 추출 로직 (헤더 -> 쿠키)
        val headerAuthorization = rq.getHeader("Authorization", "")

        // Authorization 헤더가 있는 경우
        if (!headerAuthorization.isBlank()) {
            if (!headerAuthorization.startsWith("Bearer ")) throw ServiceException(
                "401-2",
                "Authorization 헤더가 Bearer 형식이 아닙니다."
            )

            val parts = headerAuthorization.split(" ", limit = 3)

            // Bearer 뒤 값이 JWT(점 3개) 하나만 있는 경우 → accessToken으로 처리
            if (parts.size == 2 && parts[1].split(".").size == 3) {
                apiKey = ""
                accessToken = parts[1]
            } else {
                apiKey = parts[1]
                accessToken = if (parts.size == 3) parts[2] else ""
            }
        } else {
            // 헤더가 없으면 쿠키에서 조회
            apiKey = rq.getCookieValue("apiKey", "")
            accessToken = rq.getCookieValue("accessToken", "")
        }

        logger.debug("apiKey : " + apiKey)
        logger.debug("accessToken : " + accessToken)

        val isApiKeyExists = !apiKey.isBlank()
        val isAccessTokenExists = !accessToken.isBlank()

        // apiKey(리프레시 토큰) + 액세스 토큰 둘 다 없으면 패스
        if (!isApiKeyExists && !isAccessTokenExists) {
            filterChain.doFilter(request, response)
            return
        }

        var member: Member? = null
        var isAccessTokenValid = false

        // 액세스 토큰 검증
        if (isAccessTokenExists) {
            memberService.payload(accessToken)?.let { payload ->
                member = Member(
                    payload["id"] as Int,
                    payload["username"] as String,
                    payload["name"] as String,
                    from(payload["role"] as? String)
                )
                isAccessTokenValid = true
            }

        }

        // 액세스 토큰이 없거나 실패하면 apiKey로 사용자 인증
        if (member == null) {
            member = memberService
                .findByApiKey(apiKey)
                .orElseThrow { ServiceException("401-3", "API 키가 유효하지 않습니다.") }
        }

        // 액세스 토큰은 있는데 기간이 만료되었다면(유효하지 않다면) 재발급
        if (isAccessTokenExists && !isAccessTokenValid) {
            // 액세스 토큰 생성
            val actorAccessToken = memberService.genAccessToken(member)

            // 액세스 토큰을 쿠키와 헤더에 저장
            rq.setCookie("accessToken", actorAccessToken)
            rq.setHeader("Authorization", actorAccessToken)
        }

        when (member!!.status) {
            // 영구 정지 회원 JWT 차단
            MemberStatus.BANNED -> throw ServiceException("403-4", "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.")
            // 탈퇴 회원 JWT 차단
            MemberStatus.WITHDRAWN -> throw ServiceException("403-5", "탈퇴한 계정입니다.")
            else -> Unit
        }

        // Spring Security 인증 객체 생성 ⭐
        val user: UserDetails = SecurityUser(
            member.getId(),
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
}