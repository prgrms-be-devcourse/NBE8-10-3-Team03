package com.back.domain.search.search.service

import com.back.domain.post.post.dto.PostListResponse
import com.back.domain.post.post.repository.PostRepository
import com.back.domain.search.search.dto.UnifiedSearchResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class PostSearchProvider(
    private val postRepository: PostRepository
) : SearchProvider {
    override fun search(keyword: String, pageable: Pageable): List<UnifiedSearchResponse> {
        // SearchService에 있던 Post 변환 로직을 그대로 이동
        return postRepository.search(keyword, pageable).content.map { post ->
            UnifiedSearchResponse(
                id = post.id as Int,
                type = "POST",
                title = post.title,
                price = post.price,
                status = post.status.name,
                statusDisplayName = post.status.displayName,
                categoryName = post.category?.name ?: "미지정",
                thumbnailUrl = post.postImages.firstOrNull()?.image?.url,
                createDate = post.createDate,
                viewCount = post.viewCount,
                sellerId = post.seller.id as? Int ?: 0,
                sellerNickname = post.seller.nickname ?: "알 수 없는 사용자",
                sellerBadge = PostListResponse.calculateBadge(post.seller.reputation?.score)
            )
        }
    }
}