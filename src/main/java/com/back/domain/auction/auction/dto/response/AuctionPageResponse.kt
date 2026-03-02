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

        @JvmStatic
        fun of(
            content: List<AuctionListItemDto>,
            page: Int,
            size: Int,
            totalElements: Long
        ): AuctionPageResponse {
            val totalPages = if (totalElements == 0L) 0 else ((totalElements + size - 1) / size).toInt()
            return AuctionPageResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages
            )
        }
    }
}
