package com.back.domain.search.search.dto

import java.time.LocalDateTime

data class SearchCursorResponse(
    val score: Double? = null,
    val createDate: LocalDateTime? = null,
    val id: Long? = null
)