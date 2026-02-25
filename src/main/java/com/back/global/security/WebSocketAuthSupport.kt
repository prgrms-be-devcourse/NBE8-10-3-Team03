package com.back.global.security

import com.back.domain.member.member.enums.Role
import com.back.global.exception.ServiceException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

@Component
class WebSocketAuthSupport {
    fun toAuthentication(payload: Map<String, Any>): Authentication {
        val user = toSecurityUser(payload)
        return UsernamePasswordAuthenticationToken(user, null, user.authorities)
    }

    private fun toSecurityUser(payload: Map<String, Any>): SecurityUser {
        val id = (payload["id"] as? Number)?.toInt()
            ?: throw ServiceException("401-1", "인증 정보가 올바르지 않습니다.")
        val username = payload["username"] as? String
            ?: throw ServiceException("401-1", "인증 정보가 올바르지 않습니다.")
        val name = payload["name"] as? String
            ?: throw ServiceException("401-1", "인증 정보가 올바르지 않습니다.")
        val roleName = payload["role"] as? String
            ?: throw ServiceException("401-1", "인증 정보가 올바르지 않습니다.")

        val role = Role.from(roleName)
        return SecurityUser(
            id = id,
            username = username,
            password = "",
            nickname = name,
            role = role,
            authorities = listOf(SimpleGrantedAuthority(role.name)),
        )
    }
}
