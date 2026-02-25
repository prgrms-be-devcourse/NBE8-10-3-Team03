package com.back.domain.auction.auction.service.adapter

import com.back.domain.auction.auction.service.port.BidReadPort
import com.back.domain.auction.auction.service.port.HighestBidInfo
import com.back.domain.bid.bid.repository.BidRepository
import org.springframework.stereotype.Component

// BidRepository 결과를 경매 도메인이 이해하는 읽기 모델(HighestBidInfo)로 변환한다.
@Component
class BidReadAdapter(
    private val bidRepository: BidRepository
) : BidReadPort {
    override fun findHighestBidByAuctionId(auctionId: Int): HighestBidInfo? =
        bidRepository.findTopByAuctionIdOrderByPriceDesc(auctionId)
            .map { bid ->
                HighestBidInfo(
                    bidderId = bid.bidder.id,
                    bidderNickname = bid.bidder.nickname,
                    price = bid.price
                )
            }
            .orElse(null)
}
