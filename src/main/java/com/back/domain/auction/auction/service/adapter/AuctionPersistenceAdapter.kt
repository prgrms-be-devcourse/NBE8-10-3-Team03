package com.back.domain.auction.auction.service.adapter

import com.back.domain.auction.auction.dto.response.AuctionListProjection
import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.auction.auction.service.port.AuctionPersistencePort
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
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

    override fun findSliceAll(pageable: Pageable): Slice<Auction> =
        auctionRepository.findSliceBy(pageable)

    override fun findSliceByCategoryId(categoryId: Int, pageable: Pageable): Slice<Auction> =
        auctionRepository.findSliceByCategoryId(categoryId, pageable)

    override fun findSliceByStatus(status: AuctionStatus, pageable: Pageable): Slice<Auction> =
        auctionRepository.findSliceByStatus(status, pageable)

    override fun findSliceByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus, pageable: Pageable): Slice<Auction> =
        auctionRepository.findSliceByCategoryIdAndStatus(categoryId, status, pageable)

    override fun findSliceProjectionAll(pageable: Pageable): Slice<AuctionListProjection> =
        auctionRepository.findSliceProjectionBy(pageable)

    override fun findSliceProjectionByCategoryId(
        categoryId: Int,
        categoryName: String,
        pageable: Pageable
    ): Slice<AuctionListProjection> =
        auctionRepository.findSliceProjectionByCategoryId(categoryId, categoryName, pageable)

    override fun findSliceProjectionByStatus(status: AuctionStatus, pageable: Pageable): Slice<AuctionListProjection> =
        auctionRepository.findSliceProjectionByStatus(status, pageable)

    override fun findSliceProjectionByCategoryIdAndStatus(
        categoryId: Int,
        status: AuctionStatus,
        categoryName: String,
        pageable: Pageable
    ): Slice<AuctionListProjection> =
        auctionRepository.findSliceProjectionByCategoryIdAndStatus(categoryId, status, categoryName, pageable)

    override fun countAll(): Long = auctionRepository.count()

    override fun countByStatus(status: AuctionStatus): Long = auctionRepository.countByStatus(status)

    override fun countByCategoryId(categoryId: Int): Long = auctionRepository.countByCategoryId(categoryId)

    override fun countByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus): Long =
        auctionRepository.countByCategoryIdAndStatus(categoryId, status)

    override fun findExpiredOpenAuctions(now: LocalDateTime): List<Auction> =
        auctionRepository.findByStatusAndEndAtBefore(AuctionStatus.OPEN, now)
}
