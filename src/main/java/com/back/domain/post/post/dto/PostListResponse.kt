package com.back.domain.post.post.dto

import com.back.domain.post.post.entity.Post
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class PostListResponse(
    val id: Int,
    val title: String,
    val price: Int,
    val categoryName: String,
    val thumbnailUrl: String?,
    val createDate: LocalDateTime,
    val status: String,
    val statusDisplayName: String
//    val viewCount: Long,
//    val sellerId: Int,
//    val sellerNickname: String,
//    @field:Schema(description = "판매자 등급 뱃지 (안전/우수/일반/주의)", example = "안전한 판매자")
//    val sellerBadge: String
) {
    // Entity를 받아서 DTO로 변환하는 부 생성자(Constructor)
    constructor(post: Post) : this(
        id = post.id as Int, // BaseEntity의 id 타입에 따라 (Int) 캐스팅이 필요할 수 있습니다.
        title = post.title,
        price = post.price,
        categoryName = post.category?.name ?: "미지정",
        // 코틀린의 안전한 호출(?.)과 firstOrNull()을 사용하면 널 체크가 매우 우아해집니다.
        thumbnailUrl = post.postImages.firstOrNull()?.image?.url,
        createDate = post.createDate,
        status = post.status.name,
        statusDisplayName = post.status.displayName
//        viewCount = post.viewCount,
//        sellerId = post.seller?.id as? Int ?: 0,
//        sellerNickname = post.seller.nickname,
//        sellerBadge = calculateBadge(post.seller.reputation?.score)
    )

    companion object {
        fun calculateBadge(score: Double?): String {
            if (score == null) return "일반 판매자"
            return when {
                score >= 80 -> "안전한 판매자"
                score >= 60 -> "우수 판매자"
                score >= 40 -> "일반 판매자"
                else -> "주의 판매자"
            }
        }

        fun from(post: Post): PostListResponse =
            PostListResponse(post)
    }
}