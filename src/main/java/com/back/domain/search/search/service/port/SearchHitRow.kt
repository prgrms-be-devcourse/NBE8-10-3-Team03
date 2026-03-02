package com.back.domain.search.search.service.port

import java.time.LocalDateTime

interface SearchHitRow {
    val id: Int
    val type: String
    val createDate: LocalDateTime
    val typeRank: Int
}