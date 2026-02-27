package com.back.domain.search.search.dto

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.post.post.dto.PostListResponse
import com.back.domain.post.post.entity.Post
import com.back.domain.search.search.dto.UnifiedSearchResponse

    // Post -> UnifiedSearchResponse 변환 확장 함수
    fun Post.toUnifiedResponse() = UnifiedSearchResponse(
        id = this.id as Int,
        type = "POST",
        title = this.title,
        price = this.price,
        status = this.status.name,
        statusDisplayName = this.status.displayName,
        categoryName = this.category?.name ?: "미지정",
        thumbnailUrl = this.postImages.firstOrNull()?.image?.url,
        createDate = this.createDate,
        viewCount = this.viewCount,
        sellerId = this.seller.id as? Int ?: 0,
        sellerNickname = this.seller.nickname ?: "알 수 없는 사용자",
        sellerBadge = PostListResponse.calculateBadge(this.seller.reputation?.score)
    )

    // Auction -> UnifiedSearchResponse 변환 확장 함수
    fun Auction.toUnifiedResponse() = UnifiedSearchResponse(
        id = this.id,
        type = "AUCTION",
        title = this.name,
        price = requireNotNull(this.startPrice) { "Auction(${this.id}) startPrice is null" },
        status = this.status.name,
        categoryName = this.category.name,
        thumbnailUrl = this.auctionImages.firstOrNull()?.image?.url,
        createDate = this.createDate
    )
