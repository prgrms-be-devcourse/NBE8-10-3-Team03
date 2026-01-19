package com.back.domain.auction.auction.repository;

import com.back.domain.auction.auction.entity.AuctionImage;
import com.back.domain.auction.auction.entity.AuctionImageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionImageRepository extends JpaRepository<AuctionImage, AuctionImageId> {
}

