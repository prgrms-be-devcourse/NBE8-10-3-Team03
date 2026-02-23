package com.back.domain.post.post.dto

import com.back.domain.post.post.entity.Post
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PostDetailResponse(
    val id: Int,
    val title: String,
    val content: String,
    val price: Int,
    val status: String,
    val categoryName: String,
    val sellerId: Int,
    val sellerNickname: String,
    @field:Schema(description = "판매자 등급 뱃지 (안전/우수/일반/주의)", example = "안전한 판매자")
    val sellerBadge: String,
    @field:Schema(description = "판매자 실제 신용 점수 (상세에서만 노출)", example = "85.5")
    val sellerScore: Double,
    val imageUrls: List<String>,
    val createDate: LocalDateTime,
    val viewCount: Long
) {
    constructor(post: Post) : this(
        id = post.id as Int,
        title = post.title,
        content = post.content,
        price = post.price,
        status = post.status.name,
        categoryName = post.category.name,
        sellerId = post.seller.id as Int,
        sellerNickname = post.seller.nickname,
        sellerBadge = PostListResponse.calculateBadge(post.seller.reputation?.score),
        sellerScore = post.seller.reputation?.score ?: 0.0,
        imageUrls = post.postImages.map { it.image.url }, // map 함수로 리스트를 쉽게 변환
        createDate = post.createDate,
        viewCount = post.viewCount
    )
}