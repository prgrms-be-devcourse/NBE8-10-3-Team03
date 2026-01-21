package com.back.domain.bid.bid.repository;

import com.back.domain.bid.bid.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Integer> {

    // 특정 경매의 입찰 목록 조회 (최신순)
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.createdAt DESC")
    Page<Bid> findByAuctionId(@Param("auctionId") Integer auctionId, Pageable pageable);

    // 특정 경매의 최고 입찰가 조회
    @Query(value = "SELECT * FROM bids b WHERE b.auction_id = :auctionId ORDER BY b.price DESC LIMIT 1", nativeQuery = true)
    Optional<Bid> findTopByAuctionIdOrderByPriceDesc(@Param("auctionId") Integer auctionId);

    // 특정 사용자가 특정 경매에 입찰했는지 확인
    boolean existsByAuctionIdAndBidderId(Integer auctionId, Integer bidderId);
}

