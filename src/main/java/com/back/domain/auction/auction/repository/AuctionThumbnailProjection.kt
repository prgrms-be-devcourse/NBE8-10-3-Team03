package com.back.domain.auction.auction.repository

interface AuctionThumbnailProjection {
    val auctionId: Int
    val thumbnailUrl: String
}
