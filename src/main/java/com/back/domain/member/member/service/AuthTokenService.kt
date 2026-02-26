package com.back.domain.member.member.service

import com.back.domain.member.member.entity.Member
import com.back.standard.util.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AuthTokenService(
    // constructor parameter target를 고정해 Kotlin 기본 타겟 변경 경고를 방지한다.
    @param:Value("\${custom.jwt.secretKey}")
    private val jwtSecretKey: String,

    // constructor parameter target를 고정해 Kotlin 기본 타겟 변경 경고를 방지한다.
    @param:Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds: Int
) {

    fun genAccessToken(member: Member): String {
        val id = member.id
        val username = member.username
        val name = member.nickname
        val role = member.role?.name ?: throw NullPointerException("Member role is required.")

        return Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            // Ut.jwt.toString 시그니처가 MutableMap<String, Any>를 요구한다.
            mutableMapOf<String, Any>("id" to id, "username" to username, "name" to name, "role" to role)
        )
    }

    fun payload(accessToken: String?): Map<String, Any>? {
        val parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken) ?: return null

        return mutableMapOf(
            "id" to parsedPayload["id"] as Int,
            "username" to parsedPayload["username"] as String,
            "name" to parsedPayload["name"] as String,
            "role" to parsedPayload["role"] as String
        )
    }
}
