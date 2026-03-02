package com.back.domain.search.search.service.projection

import java.time.LocalDateTime

interface SearchHitRow {
    val id: Int
    val type: String        // 'AUCTION' or 'POST'
    val createDate: LocalDateTime
    val typeRank: Int       // AUCTION=1, POST=0
}