package com.back.domain.bid.bid.dto.response

import org.springframework.data.domain.Page

class BidPageResponse(
    val content: List<BidListItemDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        @JvmStatic
        fun from(page: Page<BidListItemDto>): BidPageResponse =
            BidPageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
    }
}
