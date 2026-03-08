package com.back.domain.post.post.dto

import com.back.domain.auction.auction.dto.response.AuctionListItemDto
import com.back.domain.auction.auction.dto.response.SellerDto
import com.back.domain.post.post.entity.PostStatus
import java.time.LocalDateTime

data class PostListProjection(
    val id: Int,
    val title: String,
    val price: Int,
    val categoryName: String?,
    val thumbnailUrl: String?,
    val createDate: LocalDateTime,
    val status: PostStatus
) {
    fun toDto(): PostListResponse = PostListResponse(
        id = id as Int,
        title = title,
        price = price,
        categoryName = categoryName ?: "미지정",
        thumbnailUrl = thumbnailUrl,
        createDate = createDate,
        status = status,
        statusDisplayName = status.displayName
    )
}