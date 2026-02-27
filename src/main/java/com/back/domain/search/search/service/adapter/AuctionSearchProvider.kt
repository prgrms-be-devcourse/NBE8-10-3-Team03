package com.back.domain.search.search.service.adapter

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.search.search.dto.UnifiedSearchResponse
import com.back.domain.search.search.dto.toUnifiedResponse
import com.back.domain.search.search.service.port.SearchProvider
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class AuctionSearchProvider(
    private val auctionRepository: AuctionRepository
) : SearchProvider {
    override fun search(keyword: String, pageable: Pageable): List<UnifiedSearchResponse> {
        // Repository에서 결과를 가져와 선언적으로 변환합니다.
        return auctionRepository.search(keyword, pageable)
            .content
            .map { it.toUnifiedResponse() } // 확장 함수를 사용하여 매핑 로직 분리
    }
}