package com.back.domain.auction.auction.repository

import com.back.domain.auction.auction.entity.AuctionImage
import com.back.domain.auction.auction.entity.AuctionImageId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AuctionImageRepository : JpaRepository<AuctionImage, AuctionImageId> {
    @Query(
        """
        SELECT ai.auction.id AS auctionId, ai.image.url AS thumbnailUrl
        FROM AuctionImage ai
        WHERE ai.auction.id IN :auctionIds
          AND ai.image.id = (
            SELECT MIN(ai2.image.id)
            FROM AuctionImage ai2
            WHERE ai2.auction.id = ai.auction.id
          )
        """
    )
    fun findThumbnailUrlsByAuctionIds(@Param("auctionIds") auctionIds: Collection<Int>): List<AuctionThumbnailProjection>
}
