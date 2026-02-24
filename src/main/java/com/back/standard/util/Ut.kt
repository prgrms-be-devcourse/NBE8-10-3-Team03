package com.back.standard.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import tools.jackson.databind.ObjectMapper
import java.security.Key
import java.util.*

class Ut {
    object jwt {
        fun toString(secret: String, expireSeconds: Int, body: MutableMap<String?, Any?>): String? {
            val claimsBuilder = Jwts.claims()

            for (entry in body.entries) {
                claimsBuilder.add(entry.key, entry.value)
            }

            val claims = claimsBuilder.build()

            val issuedAt = Date()
            val expiration = Date(issuedAt.getTime() + 1000L * expireSeconds)

            val secretKey: Key = Keys.hmacShaKeyFor(secret.toByteArray())

            val jwt = Jwts.builder()
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()

            return jwt
        }

        fun isValid(secret: String, jwtStr: String?): Boolean {
            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

            try {
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(jwtStr)
            } catch (e: Exception) {
                return false
            }

            return true
        }

        fun payload(secret: String, jwtStr: String?): MutableMap<String?, Any?>? {
            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

            try {
                return LinkedHashMap<String?, Any?>(
                    Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(jwtStr)
                        .getPayload()
                )
            } catch (e: Exception) {
                return null
            }
        }
    }

    object json {
        var objectMapper: ObjectMapper? = null

        @JvmOverloads
        fun toString(`object`: Any?, defaultValue: String? = null): String? {
            try {
                return objectMapper!!.writeValueAsString(`object`)
            } catch (e: Exception) {
                return defaultValue
            }
        }
    }
}
