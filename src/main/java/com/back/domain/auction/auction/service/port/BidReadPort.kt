package com.back.domain.auction.auction.service.port

// 스케줄러/서비스에서 입찰 도메인 엔티티를 직접 의존하지 않도록 최소 정보만 전달한다.
data class HighestBidInfo(
    val bidderId: Int,
    val bidderNickname: String,
    val price: Int
)

// Outbound port: 경매 관점에서 필요한 "최고 입찰 조회" 기능만 노출한다.
interface BidReadPort {
    fun findHighestBidByAuctionId(auctionId: Int): HighestBidInfo?
}
