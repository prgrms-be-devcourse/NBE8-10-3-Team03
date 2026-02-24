package com.back.global.rq

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import lombok.SneakyThrows
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import java.util.*
import java.util.function.Function
import java.util.function.Predicate

@Component
@RequiredArgsConstructor
class Rq {
    private val req: HttpServletRequest? = null
    private val resp: HttpServletResponse? = null
    private val memberService: MemberService? = null

    val actor: Member?
        get() = Optional.ofNullable<Authentication?>(
            SecurityContextHolder
                .getContext()
                .getAuthentication()
        )
            .map<Any?>(Function { obj: Authentication? -> obj!!.getPrincipal() })
            .filter(Predicate { principal: Any? -> principal is SecurityUser })
            .map<SecurityUser?>(Function { principal: Any? -> principal as SecurityUser })
            .map<Member?>(Function { securityUser: SecurityUser? ->
                Member(
                    securityUser!!.id,
                    securityUser.getUsername(),
                    securityUser.getName(),
                    securityUser.role
                )
            })
            .orElse(null)

    fun getHeader(name: String?, defaultValue: String?): String? {
        return Optional
            .ofNullable<String?>(req!!.getHeader(name))
            .filter(Predicate { headerValue: String? -> !headerValue!!.isBlank() })
            .orElse(defaultValue)
    }

    fun setHeader(name: String?, value: String?) {
        var value = value
        if (value == null) value = ""

        if (value.isBlank()) {
            req!!.removeAttribute(name)
        } else {
            resp!!.setHeader(name, value)
        }
    }

    fun getCookieValue(name: String?, defaultValue: String?): String? {
        return Optional
            .ofNullable<Array<Cookie?>?>(req!!.getCookies())
            .flatMap<String?>(
                Function { cookies: Array<Cookie?>? ->
                    Arrays.stream<Cookie?>(cookies)
                        .filter { cookie: Cookie? -> cookie!!.getName() == name }
                        .map<String?> { obj: Cookie? -> obj!!.getValue() }
                        .filter { value: String? -> !value!!.isBlank() }
                        .findFirst()
                }
            )
            .orElse(defaultValue)
    }

    fun setCookie(name: String?, value: String?) {
        var value = value
        if (value == null) value = ""

        val cookie = Cookie(name, value)
        cookie.setPath("/")
        cookie.setHttpOnly(true)
        cookie.setDomain("localhost")
        cookie.setSecure(true)
        cookie.setAttribute("SameSite", "Strict")

        if (value.isBlank()) cookie.setMaxAge(0)
        else cookie.setMaxAge(60 * 60 * 24 * 365)

        resp!!.addCookie(cookie)
    }

    fun deleteCookie(name: String?) {
        setCookie(name, null)
    }

    @SneakyThrows
    fun sendRedirect(url: String?) {
        resp!!.sendRedirect(url)
    }

    val actorFromDb: Member?
        get() {
            val actor = this.actor

            if (actor == null) {
                return null
            }

            return memberService!!.findById(actor.getId()).get()
        }
}
