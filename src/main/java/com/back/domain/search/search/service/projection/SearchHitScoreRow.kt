package com.back.domain.search.search.service.projection

import java.time.LocalDateTime

interface SearchHitScoreRow {
    val id: Int
    val type: String
    val createDate: LocalDateTime
    val typeRank: Int
    val score: Double
}