package com.back.domain.auction.auction.dto.response

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.entity.CancellerRole
import java.time.LocalDateTime

class AuctionDetailResponse() {
    var auctionId: Int? = null
    var name: String? = null
    var description: String? = null
    var startPrice: Int? = null
    var currentHighestBid: Int? = null
    var buyNowPrice: Int? = null
    var bidCount: Int? = null
    var status: AuctionStatus? = null
    var startAt: LocalDateTime? = null
    var endAt: LocalDateTime? = null
    var imageUrls: List<String?> = emptyList()
    var seller: SellerDto? = null
    var categoryName: String? = null

    var winnerId: Int? = null
    var closedAt: LocalDateTime? = null

    var cancelledBy: Int? = null
    var cancellerRole: CancellerRole? = null
    var cancellerRoleDescription: String? = null

    constructor(auction: Auction) : this() {
        auctionId = auction.id
        name = auction.name
        description = auction.description
        startPrice = auction.startPrice
        currentHighestBid = auction.currentHighestBid
        buyNowPrice = auction.buyNowPrice
        bidCount = auction.bidCount
        status = auction.status
        startAt = auction.startAt
        endAt = auction.endAt
        categoryName = auction.category.name

        winnerId = auction.winnerId
        closedAt = auction.closedAt

        cancelledBy = auction.cancelledBy
        cancellerRole = auction.cancellerRole
        cancellerRoleDescription = auction.cancellerRole?.description

        imageUrls = auction.auctionImages.map { it.image.url }

        val reputationScore = auction.seller.reputation?.score
        seller = SellerDto(
            auction.seller.id,
            auction.seller.nickname,
            reputationScore
        )
    }
}
