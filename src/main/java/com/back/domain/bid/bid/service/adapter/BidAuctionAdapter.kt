package com.back.domain.bid.bid.service.adapter

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.bid.bid.service.port.BidAuctionPort
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component

@Component
class BidAuctionAdapter(
    private val auctionRepository: AuctionRepository
) : BidAuctionPort {
    override fun getAuctionWithLockOrThrow(auctionId: Int): Auction =
        auctionRepository.findByIdWithLock(auctionId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 경매입니다.") }

    override fun existsAuction(auctionId: Int): Boolean = auctionRepository.existsById(auctionId)

    override fun saveAuction(auction: Auction) {
        auctionRepository.save(auction)
    }
}
