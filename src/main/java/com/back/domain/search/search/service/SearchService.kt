package com.back.domain.search.search.service

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.post.post.dto.PostListResponse
import com.back.domain.post.post.repository.PostRepository
import com.back.domain.search.search.dto.UnifiedSearchResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SearchService(
    private val postRepository: PostRepository,
    private val auctionRepository: AuctionRepository
) {
    // 통합 검색 (POST + AUCTION)
    fun searchUnified(keyword: String, pageable: Pageable): Page<UnifiedSearchResponse> {
        val posts = postRepository.search(keyword, pageable)
        val auctions = auctionRepository.search(keyword, pageable)

        val combinedResults = mutableListOf<UnifiedSearchResponse>()

        // 1. Post 변환
        posts.forEach { post ->
            combinedResults.add(
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
            )
        }

        // 2. Auction 변환 (Auction은 아직 Java 코드)
        auctions.forEach { auction ->
            combinedResults.add(
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
            )
        }

        // 3. 최신순 정렬 (코틀린의 간결한 정렬 문법)
        val sortedResults = combinedResults.sortedByDescending { it.createDate }

        // 4. 페이징 처리
        val start = pageable.offset.toInt()
        val end = (start + pageable.pageSize).coerceAtMost(sortedResults.size)

        val pagedResults = if (start >= sortedResults.size) {
            emptyList()
        } else {
            sortedResults.subList(start, end)
        }

        return PageImpl(pagedResults, pageable, sortedResults.size.toLong())
    }
}
