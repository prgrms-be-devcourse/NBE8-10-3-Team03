package com.back.domain.member.member.service

import com.back.domain.member.member.entity.Member
import com.back.standard.util.Ut
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Map

@Service
class AuthTokenService(
    @Value("\${custom.jwt.secretKey}")
    private val jwtSecretKey: String,

    @Value("\${custom.accessToken.expirationSeconds}")
    private val accessTokenExpirationSeconds: Int
) {

    fun genAccessToken(member: Member): String {
        val id = member.id
        val username = member.username
        val name = member.nickname
        val role = member.role?.name

        return Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            Map.of<String, Any>("id", id, "username", username, "name", name, "role", role)
        )
    }

    fun payload(accessToken: String?): MutableMap<String, Any>? {
        val parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken) ?: return null

        return mapOf(
            "id" to parsedPayload["id"] as Int,
            "username" to parsedPayload["username"] as String,
            "name" to parsedPayload["name"] as String,
            "role" to parsedPayload["role"] as String
        ) as MutableMap<String, Any>?
    }
}