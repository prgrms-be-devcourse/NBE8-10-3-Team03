package com.back.domain.bid.bid.service.adapter

import com.back.domain.bid.bid.entity.Bid
import com.back.domain.bid.bid.repository.BidRepository
import com.back.domain.bid.bid.service.port.BidPersistencePort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class BidPersistenceAdapter(
    private val bidRepository: BidRepository
) : BidPersistencePort {
    override fun save(bid: Bid): Bid = bidRepository.save(bid)

    override fun findTopByAuctionIdOrderByPriceDesc(auctionId: Int): Optional<Bid> =
        bidRepository.findTopByAuctionIdOrderByPriceDesc(auctionId)

    override fun findByAuctionId(auctionId: Int, pageable: Pageable): Page<Bid> =
        bidRepository.findByAuctionId(auctionId, pageable)
}
