package com.back.domain.image.image.repository

import com.back.domain.image.image.entity.Image
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ImageRepository : JpaRepository<Image, Int> {
    @Query(
        "SELECT pi.post.id, i.url FROM PostImage pi JOIN pi.image i " +
            "WHERE pi.post.id IN :ids AND pi.image.id = (SELECT MIN(pi2.image.id) FROM PostImage pi2 WHERE pi2.post.id = pi.post.id)"
    )
    fun findPostMainImages(@Param("ids") ids: List<Int>): List<Array<Any?>>

    @Query(
        "SELECT ai.auction.id, i.url FROM AuctionImage ai JOIN ai.image i " +
            "WHERE ai.auction.id IN :ids AND ai.image.id = (SELECT MIN(ai2.image.id) FROM AuctionImage ai2 WHERE ai2.auction.id = ai.auction.id)"
    )
    fun findAuctionMainImages(@Param("ids") ids: List<Int>): List<Array<Any?>>
}
