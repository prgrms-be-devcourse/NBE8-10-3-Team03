package com.back.domain.auction.auction.dto.response

import org.springframework.data.domain.Page

data class AuctionPageResponse(
    val content: List<AuctionListItemDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
) {
    companion object {
        @JvmStatic
        fun from(page: Page<AuctionListItemDto>): AuctionPageResponse =
            AuctionPageResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages
            )
    }
}
