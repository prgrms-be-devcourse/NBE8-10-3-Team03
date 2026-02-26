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
/*
    private val postRepository: PostRepository,
    private val auctionRepository: AuctionRepository

 */
    private val searchProviders: List<SearchProvider>
) {

    // 통합 검색 (POST + AUCTION)
    fun searchUnified(keyword: String, pageable: Pageable): Page<UnifiedSearchResponse> {

        // 1. 모든 도메인(Post, Auction 등)에서 검색 결과 획득 및 병합
        val combinedResults = searchProviders.flatMap { it.search(keyword, pageable) }
            .sortedByDescending { it.createDate } // 2. 최신순 정렬

        // 3. 코틀린스러운 컬렉션 함수를 이용한 안전한 페이징 처리
        val start = pageable.offset.toInt()
        val pagedResults = combinedResults.drop(start).take(pageable.pageSize)

        return PageImpl(pagedResults, pageable, combinedResults.size.toLong())
/*
        val posts = postRepository.search(keyword, pageable)
        val auctions = auctionRepository.search(keyword, pageable)

        val combinedResults = mutableListOf<UnifiedSearchResponse>()

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

        // 최신순 정렬
        val sortedResults = combinedResults.sortedByDescending { it.createDate }

        // 페이징 처리
        val start = pageable.offset.toInt()
        val end = (start + pageable.pageSize).coerceAtMost(sortedResults.size)

        val pagedResults = if (start >= sortedResults.size) {
            emptyList()
        } else {
            sortedResults.subList(start, end)
        }

        return PageImpl(pagedResults, pageable, sortedResults.size.toLong())

 */
    }
}
