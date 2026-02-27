package com.back.domain.auction.auction.repository

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Modifying
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
    fun findByCategoryName(categoryName: String, pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findByStatus(status: AuctionStatus, pageable: Pageable): Page<Auction>

    @EntityGraph(attributePaths = ["seller", "seller.reputation", "category"])
    fun findByCategoryNameAndStatus(categoryName: String, status: AuctionStatus, pageable: Pageable): Page<Auction>

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

    @Modifying
    fun deleteByNameStartingWith(prefix: String): Long

    @Query("SELECT a.id FROM Auction a WHERE a.name LIKE CONCAT(:prefix, '%')")
    fun findIdsByNameStartingWith(@Param("prefix") prefix: String): List<Int>
}
