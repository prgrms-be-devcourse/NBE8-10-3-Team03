package com.back.domain.bid.bid.repository

import com.back.domain.bid.bid.entity.Bid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface BidRepository : JpaRepository<Bid, Int> {
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.createdAt DESC")
    fun findByAuctionId(@Param("auctionId") auctionId: Int, pageable: Pageable): Page<Bid>

    @Query(
        value = "SELECT * FROM bids b WHERE b.auction_id = :auctionId ORDER BY b.price DESC LIMIT 1",
        nativeQuery = true
    )
    fun findTopByAuctionIdOrderByPriceDesc(@Param("auctionId") auctionId: Int): Optional<Bid>

    fun existsByAuctionIdAndBidderId(auctionId: Int, bidderId: Int): Boolean
}
