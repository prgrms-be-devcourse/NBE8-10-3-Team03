package com.back.domain.search.search.dto


data class SearchCursor(
    val v: Int = 1,
    val sort: String,                 // relevance|newest|oldest
    val score: Double? = null,        // relevance에서만 사용
    val createDate: String,
    val typeRank: Int,                // AUCTION=1, POST=0
    val id: Int
)