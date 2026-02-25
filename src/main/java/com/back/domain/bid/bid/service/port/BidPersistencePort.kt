package com.back.domain.bid.bid.service.port

import com.back.domain.bid.bid.entity.Bid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

interface BidPersistencePort {
    fun save(bid: Bid): Bid
    fun findTopByAuctionIdOrderByPriceDesc(auctionId: Int): Optional<Bid>
    fun findByAuctionId(auctionId: Int, pageable: Pageable): Page<Bid>
}
