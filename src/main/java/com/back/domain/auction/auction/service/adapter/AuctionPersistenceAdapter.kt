package com.back.domain.auction.auction.service.adapter

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.auction.auction.service.port.AuctionPersistencePort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.LocalDateTime

// JPA 저장소를 AuctionPersistencePort로 감싸 인프라 의존을 어댑터 계층으로 한정한다.
@Component
class AuctionPersistenceAdapter(
    private val auctionRepository: AuctionRepository
) : AuctionPersistencePort {
    override fun save(auction: Auction): Auction = auctionRepository.save(auction)

    override fun delete(auction: Auction) = auctionRepository.delete(auction)

    override fun findByIdOrNull(auctionId: Int): Auction? = auctionRepository.findById(auctionId).orElse(null)

    override fun findWithDetailsByIdOrNull(auctionId: Int): Auction? =
        auctionRepository.findWithDetailsById(auctionId).orElse(null)

    override fun findAll(pageable: Pageable): Page<Auction> = auctionRepository.findAll(pageable)

    override fun findBySellerId(sellerId: Int, pageable: Pageable): Page<Auction> =
        auctionRepository.findBySellerId(sellerId, pageable)

    override fun findBySellerIdAndStatus(sellerId: Int, status: AuctionStatus, pageable: Pageable): Page<Auction> =
        auctionRepository.findBySellerIdAndStatus(sellerId, status, pageable)

    override fun findByCategoryId(categoryId: Int, pageable: Pageable): Page<Auction> =
        auctionRepository.findByCategoryId(categoryId, pageable)

    override fun findByStatus(status: AuctionStatus, pageable: Pageable): Page<Auction> =
        auctionRepository.findByStatus(status, pageable)

    override fun findByCategoryIdAndStatus(
        categoryId: Int,
        status: AuctionStatus,
        pageable: Pageable
    ): Page<Auction> = auctionRepository.findByCategoryIdAndStatus(categoryId, status, pageable)

    override fun findExpiredOpenAuctions(now: LocalDateTime): List<Auction> =
        auctionRepository.findByStatusAndEndAtBefore(AuctionStatus.OPEN, now)
}
