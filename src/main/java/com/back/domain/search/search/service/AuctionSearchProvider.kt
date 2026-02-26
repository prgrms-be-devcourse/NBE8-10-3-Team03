package com.back.domain.search.search.service

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.search.search.dto.UnifiedSearchResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class AuctionSearchProvider(
    private val auctionRepository: AuctionRepository
) : SearchProvider {
    override fun search(keyword: String, pageable: Pageable): List<UnifiedSearchResponse> {
        // SearchService에 있던 Auction 변환 로직을 그대로 이동
        return auctionRepository.search(keyword, pageable).content.map { auction ->
            UnifiedSearchResponse(
                id = auction.id,
                type = "AUCTION",
                title = auction.name,
                price = requireNotNull(auction.startPrice) { "Auction(${auction.id}) startPrice is null" },
                status = auction.status.name,
                categoryName = auction.category.name,
                thumbnailUrl = auction.auctionImages.firstOrNull()?.image?.url,
                createDate = auction.createDate
            )
        }
    }
}