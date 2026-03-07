package com.back.domain.search.search.service

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64

data class DateCursor(
    val createDate: LocalDateTime,
    val id: Int
) {
    fun encode(): String {
        val raw = listOf(createDate, id).joinToString("|")
        return Base64.getUrlEncoder().encodeToString(raw.toByteArray(StandardCharsets.UTF_8))
    }

    companion object {
        fun decode(cursor: String?): DateCursor? {
            if (cursor.isNullOrBlank()) return null

            val decoded = String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            val parts = decoded.split("|")
            require(parts.size == 2) { "잘못된 date cursor 형식입니다." }

            return DateCursor(
                createDate = LocalDateTime.parse(parts[0]),
                id = parts[1].toInt()
            )
        }
    }
}