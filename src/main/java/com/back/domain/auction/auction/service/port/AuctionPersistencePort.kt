package com.back.domain.auction.auction.service.port

import com.back.domain.auction.auction.dto.response.AuctionListProjection
import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
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
    fun findSliceAll(pageable: Pageable): Slice<Auction>
    fun findSliceByCategoryId(categoryId: Int, pageable: Pageable): Slice<Auction>
    fun findSliceByStatus(status: AuctionStatus, pageable: Pageable): Slice<Auction>
    fun findSliceByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus, pageable: Pageable): Slice<Auction>
    fun findSliceProjectionAll(pageable: Pageable): Slice<AuctionListProjection>
    fun findSliceProjectionByCategoryId(
        categoryId: Int,
        categoryName: String,
        pageable: Pageable
    ): Slice<AuctionListProjection>
    fun findSliceProjectionByStatus(status: AuctionStatus, pageable: Pageable): Slice<AuctionListProjection>
    fun findSliceProjectionByCategoryIdAndStatus(
        categoryId: Int,
        status: AuctionStatus,
        categoryName: String,
        pageable: Pageable
    ): Slice<AuctionListProjection>
    fun countAll(): Long
    fun countByStatus(status: AuctionStatus): Long
    fun countByCategoryId(categoryId: Int): Long
    fun countByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus): Long
    fun findExpiredOpenAuctions(now: LocalDateTime): List<Auction>
}
