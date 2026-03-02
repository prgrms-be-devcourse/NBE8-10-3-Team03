package com.back.domain.auction.auction.repository

import com.back.domain.auction.auction.dto.response.AuctionListProjection
import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.search.search.service.projection.AuctionListRow
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface AuctionRepository : JpaRepository<Auction, Int> {
    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    override fun findAll(pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findBySellerId(sellerId: Int, pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findBySellerIdAndStatus(sellerId: Int, status: AuctionStatus, pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findByCategoryId(categoryId: Int, pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findByStatus(status: AuctionStatus, pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus, pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findSliceBy(pageable: Pageable): Slice<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findSliceByCategoryId(categoryId: Int, pageable: Pageable): Slice<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findSliceByStatus(status: AuctionStatus, pageable: Pageable): Slice<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findSliceByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus, pageable: Pageable): Slice<Auction>

    @Query(
        """
        SELECT new com.back.domain.auction.auction.dto.response.AuctionListProjection(
            a.id, a.name, a.startPrice, a.currentHighestBid, a.buyNowPrice, a.status, a.endAt, a.bidCount,
            s.id, s.nickname, r.score, c.name, a.thumbnailUrl
        )
        FROM Auction a
        JOIN a.seller s
        LEFT JOIN s.reputation r
        JOIN a.category c
        """
    )
    fun findSliceProjectionBy(pageable: Pageable): Slice<AuctionListProjection>

    @Query(
        """
        SELECT new com.back.domain.auction.auction.dto.response.AuctionListProjection(
            a.id, a.name, a.startPrice, a.currentHighestBid, a.buyNowPrice, a.status, a.endAt, a.bidCount,
            s.id, s.nickname, r.score, :categoryName, a.thumbnailUrl
        )
        FROM Auction a
        JOIN a.seller s
        LEFT JOIN s.reputation r
        WHERE a.category.id = :categoryId
        """
    )
    fun findSliceProjectionByCategoryId(
        @Param("categoryId") categoryId: Int,
        @Param("categoryName") categoryName: String,
        pageable: Pageable
    ): Slice<AuctionListProjection>

    @Query(
        """
        SELECT new com.back.domain.auction.auction.dto.response.AuctionListProjection(
            a.id, a.name, a.startPrice, a.currentHighestBid, a.buyNowPrice, a.status, a.endAt, a.bidCount,
            s.id, s.nickname, r.score, c.name, a.thumbnailUrl
        )
        FROM Auction a
        JOIN a.seller s
        LEFT JOIN s.reputation r
        JOIN a.category c
        WHERE a.status = :status
        """
    )
    fun findSliceProjectionByStatus(
        @Param("status") status: AuctionStatus,
        pageable: Pageable
    ): Slice<AuctionListProjection>

    @Query(
        """
        SELECT new com.back.domain.auction.auction.dto.response.AuctionListProjection(
            a.id, a.name, a.startPrice, a.currentHighestBid, a.buyNowPrice, a.status, a.endAt, a.bidCount,
            s.id, s.nickname, r.score, :categoryName, a.thumbnailUrl
        )
        FROM Auction a
        JOIN a.seller s
        LEFT JOIN s.reputation r
        WHERE a.category.id = :categoryId AND a.status = :status
        """
    )
    fun findSliceProjectionByCategoryIdAndStatus(
        @Param("categoryId") categoryId: Int,
        @Param("status") status: AuctionStatus,
        @Param("categoryName") categoryName: String,
        pageable: Pageable
    ): Slice<AuctionListProjection>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category", "auctionImages", "auctionImages.image"])
    fun findWithDetailsById(id: Int): Optional<Auction>

    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endAt < :now")
    fun findByStatusAndEndAtBefore(@Param("status") status: AuctionStatus, @Param("now") now: LocalDateTime): List<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category", "auctionImages", "auctionImages.image"])
    @Query("SELECT a FROM Auction a WHERE a.name LIKE %:kw% OR a.description LIKE %:kw%")
    fun search(@Param("kw") kw: String, pageable: Pageable): Page<Auction>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    fun findByIdWithLock(@Param("id") id: Int): Optional<Auction>

    fun countByNameStartingWith(prefix: String): Long
    fun countByStatus(status: AuctionStatus): Long
    fun countByCategoryId(categoryId: Int): Long
    fun countByCategoryIdAndStatus(categoryId: Int, status: AuctionStatus): Long

    fun deleteByNameStartingWith(prefix: String): Long

    @Query("SELECT a.id FROM Auction a WHERE a.name LIKE CONCAT(:prefix, '%')")
    fun findIdsByNameStartingWith(@Param("prefix") prefix: String): List<Int>

    @Query(
        value = """
          SELECT
            a.id          AS id,
            a.name        AS title,
            a.start_price AS price,
            a.status      AS status
          FROM auction a
          WHERE a.id IN (:ids)
        """,
        nativeQuery = true
    )
    fun findListRowsByIds(@Param("ids") ids: List<Int>): List<AuctionListRow>
}
