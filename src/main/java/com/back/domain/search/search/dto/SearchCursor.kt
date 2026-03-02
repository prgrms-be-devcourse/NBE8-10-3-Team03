package com.back.domain.search.search.dto

import java.time.LocalDateTime

data class SearchCursor(
    val v: Int = 1,
    val sort: String, // relevance|newest|oldest
    val score: Double? = null,
    val createDate: LocalDateTime,
    val typeRank: Int?,
    val id: Int
)