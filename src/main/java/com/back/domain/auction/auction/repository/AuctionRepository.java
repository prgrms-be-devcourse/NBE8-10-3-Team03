package com.back.domain.auction.auction.repository;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.entity.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Integer> {

    // 전체 조회 (페이징) - EntityGraph로 N+1 해결
    @EntityGraph(attributePaths = {"seller", "seller.reputation", "category"})
    Page<Auction> findAll(Pageable pageable);

    // 카테고리로 필터링
    @EntityGraph(attributePaths = {"seller", "seller.reputation", "category"})
    Page<Auction> findByCategoryName(String categoryName, Pageable pageable);

    // 상태로 필터링
    @EntityGraph(attributePaths = {"seller", "seller.reputation", "category"})
    Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);

    // 카테고리 + 상태로 필터링
    @EntityGraph(attributePaths = {"seller", "seller.reputation", "category"})
    Page<Auction> findByCategoryNameAndStatus(String categoryName, AuctionStatus status, Pageable pageable);

    // 상세 조회 - 이미지 포함
    @EntityGraph(attributePaths = {"seller", "seller.reputation", "category", "auctionImages", "auctionImages.image"})
    Optional<Auction> findWithDetailsById(Integer id);

    // 만료된 경매 조회 (낙찰 처리용)
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endAt < :now")
    List<Auction> findByStatusAndEndAtBefore(@Param("status") AuctionStatus status, @Param("now") LocalDateTime now);

    // 검색 기능 (PostRepository의 search와 동일한 형태)
    @EntityGraph(attributePaths = {"seller", "seller.reputation", "category", "auctionImages", "auctionImages.image"})
    @Query("SELECT a FROM Auction a WHERE a.name LIKE %:kw% OR a.description LIKE %:kw%")
    Page<Auction> search(@Param("kw") String kw, Pageable pageable);

    // 비관적 락으로 경매 조회 (동시성 제어용)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithLock(@Param("id") Integer id);
}
