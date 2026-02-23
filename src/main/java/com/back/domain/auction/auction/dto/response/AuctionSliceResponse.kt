package com.back.domain.auction.auction.dto.response

import org.springframework.data.domain.Slice

data class AuctionSliceResponse(
    val content: List<AuctionListItemDto>,
    val page: Int,
    val size: Int,
    val hasNext: Boolean
) {
    companion object {
        @JvmStatic
        fun from(page: Slice<AuctionListItemDto>): AuctionSliceResponse =
            AuctionSliceResponse(
                content = page.content,
                page = page.number,
                size = page.size,
                hasNext = page.hasNext()
            )
    }
}
