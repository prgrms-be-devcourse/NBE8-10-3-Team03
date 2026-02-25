package com.back.domain.chat.chat.service.adapter

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.chat.chat.service.port.ChatItemInfo
import com.back.domain.chat.chat.service.port.ChatItemPort
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.repository.PostRepository
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component

/**
 * ChatItemPort 구현체.
 * 게시글/경매 저장소에서 채팅 시작에 필요한 최소 스냅샷 정보만 추출한다.
 */
@Component
class ChatItemAdapter(
    private val postRepository: PostRepository,
    private val auctionRepository: AuctionRepository,
) : ChatItemPort {
    /** 게시글 존재/상태를 검증하고 채팅방 생성용 스냅샷을 반환한다. */
    override fun getPostItemOrThrow(postId: Int): ChatItemInfo {
        val post = postRepository.findById(postId)
            .orElseThrow { ServiceException("404-2", "해당 게시글이 존재하지 않습니다.") }

        if (post.status != PostStatus.SALE) {
            throw ServiceException("400-1", "판매 중인 상품이 아니므로 채팅을 시작할 수 없습니다.")
        }

        return ChatItemInfo(
            itemId = post.id,
            itemName = post.title,
            itemPrice = post.price,
            itemImageUrl = post.postImages.firstOrNull()?.image?.url,
            sellerApiKey = post.seller.apiKey ?: throw ServiceException("500-1", "판매자 apiKey가 없습니다."),
        )
    }

    /** 경매 존재를 검증하고 채팅방 생성용 스냅샷을 반환한다. */
    override fun getAuctionItemOrThrow(auctionId: Int): ChatItemInfo {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { ServiceException("404-3", "존재하지 않는 경매입니다.") }

        return ChatItemInfo(
            itemId = auction.id,
            itemName = auction.name,
            itemPrice = auction.currentHighestBid ?: auction.startPrice,
            itemImageUrl = auction.auctionImages.firstOrNull()?.image?.url,
            sellerApiKey = auction.seller.apiKey ?: throw ServiceException("500-1", "판매자 apiKey가 없습니다."),
        )
    }
}
