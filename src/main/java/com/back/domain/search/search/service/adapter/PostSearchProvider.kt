package com.back.domain.search.search.service.adapter

import com.back.domain.post.post.repository.PostRepository
import com.back.domain.search.search.dto.UnifiedSearchResponse
import com.back.domain.search.search.dto.toUnifiedResponse
import com.back.domain.search.search.service.port.SearchProvider
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class PostSearchProvider(
    private val postRepository: PostRepository
) : SearchProvider {
    override fun search(keyword: String, pageable: Pageable): List<UnifiedSearchResponse> {
        return postRepository.search(keyword, pageable)
            .content.map { it.toUnifiedResponse() } // 확장 함수 사용
    }
}