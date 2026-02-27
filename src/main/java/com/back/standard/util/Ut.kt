package com.back.standard.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import tools.jackson.databind.ObjectMapper
import java.security.Key
import java.util.*

class Ut {
    object jwt {
        @JvmStatic
        fun toString(secret: String, expireSeconds: Int, body: MutableMap<String, Any>): String {
            val claims = Jwts.claims().apply {
                body.forEach { (key, value) -> add(key, value) }
            }.build()

            val issuedAt = Date()
            val expiration = Date(issuedAt.getTime() + 1000L * expireSeconds)
            val secretKey: Key = Keys.hmacShaKeyFor(secret.toByteArray())

            return Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()
        }

        @JvmStatic
        fun isValid(secret: String, jwtStr: String?): Boolean {
            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

            return runCatching {
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(jwtStr)
            }.isSuccess
        }

        @JvmStatic
        fun payload(secret: String, jwtStr: String?): MutableMap<String, Any>? {
            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

            val claims = runCatching {
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(jwtStr)
                    .payload
                    .toMap()
            }.getOrNull() ?: return null

            return claims.entries
                .mapNotNull { (key, value) -> (value as? Any)?.let { key to it } }
                .toMap()
                .toMutableMap()
        }
    }

    object json {
        lateinit var objectMapper: ObjectMapper

        @JvmOverloads
        fun toString(`object`: Any?, defaultValue: String? = null): String? =
            runCatching {
                check(::objectMapper.isInitialized) { "ObjectMapper is not initialized." }
                objectMapper.writeValueAsString(`object`)
            }.getOrElse { defaultValue }
    }
}
