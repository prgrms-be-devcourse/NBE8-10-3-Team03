package com.back.domain.member.member.service

import com.back.domain.member.member.entity.Member
import com.back.standard.util.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Map

@Service
class AuthTokenService {
    @Value("\${custom.jwt.secretKey}")
    private val jwtSecretKey: String? = null

    @Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds = 0

    fun genAccessToken(member: Member): String {
        val id = member.id
        val username = member.username
        val name = member.nickname
        val role = member.role.name

        return Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            Map.of<String?, Any?>("id", id, "username", username, "name", name, "role", role)
        )
    }

    fun payload(accessToken: String?): MutableMap<String, Any>? {
        val parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken)

        if (parsedPayload == null) return null

        val id = parsedPayload.get("id") as Int
        val username = parsedPayload.get("username") as String
        val name = parsedPayload.get("name") as String
        val role = parsedPayload.get("role") as String

        return Map.of<String?, Any?>("id", id, "username", username, "name", name, "role", role)
    }
}