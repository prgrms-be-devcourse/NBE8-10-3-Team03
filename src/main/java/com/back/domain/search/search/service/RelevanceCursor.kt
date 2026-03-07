package com.back.domain.search.search.service

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64

data class RelevanceCursor(
    val score: Double,
    val createDate: LocalDateTime,
    val id: Int
) {
    fun encode(): String {
        val raw = listOf(score, createDate, id).joinToString("|")
        return Base64.getUrlEncoder().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
    }

    companion object {
        fun decode(cursor: String?): RelevanceCursor? {
            if (cursor.isNullOrBlank()) return null

            val decoded = String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            val parts = decoded.split("|")
            require(parts.size == 3) { "잘못된 relevance cursor 형식입니다." }

            return RelevanceCursor(
                score = parts[0].toDouble(),
                createDate = LocalDateTime.parse(parts[1]),
                id = parts[2].toInt()
            )
        }
    }
}