package com.back.domain.search.search.service.projection

import com.back.domain.search.search.dto.SearchCursor
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.Base64

object CursorCodec {
    private val om = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    fun encode(cursor: SearchCursor): String {
        val json = om.writeValueAsBytes(cursor)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json)
    }

    fun decode(cursor: String): SearchCursor {
        val bytes = Base64.getUrlDecoder().decode(cursor)
        return om.readValue(bytes)
    }
}