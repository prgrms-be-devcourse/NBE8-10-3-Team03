package com.back.domain.auction.auction.service.port

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

// Outbound port: 경매 영속화 접근을 애플리케이션 계층에서 추상화한다.
interface AuctionPersistencePort {
    fun save(auction: Auction): Auction
    fun delete(auction: Auction)
    fun findByIdOrNull(auctionId: Int): Auction?
    fun findWithDetailsByIdOrNull(auctionId: Int): Auction?
    fun findAll(pageable: Pageable): Page<Auction>
    fun findBySellerId(sellerId: Int, pageable: Pageable): Page<Auction>
    fun findBySellerIdAndStatus(sellerId: Int, status: AuctionStatus, pageable: Pageable): Page<Auction>
    fun findByCategoryId(categoryId: Int, pageable: Pageable): Page<Auction>
    fun findByStatus(status: AuctionStatus, pageable: Pageable): Page<Auction>
    fun findByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus, pageable: Pageable): Page<Auction>
    fun findExpiredOpenAuctions(now: LocalDateTime): List<Auction>
}
