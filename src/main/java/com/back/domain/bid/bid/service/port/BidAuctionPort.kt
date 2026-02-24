package com.back.domain.bid.bid.service.port

import com.back.domain.auction.auction.entity.Auction

interface BidAuctionPort {
    fun getAuctionWithLockOrThrow(auctionId: Int): Auction
    fun existsAuction(auctionId: Int): Boolean
    fun saveAuction(auction: Auction)
}
