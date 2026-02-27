package com.back.domain.image.image.service.port

import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.AuctionMainImageProjection
import com.back.domain.image.image.repository.PostMainImageProjection

interface ImagePort {
    fun save(image: Image): Image
    fun findPostMainImages(ids: List<Int>): List<PostMainImageProjection>
    fun findAuctionMainImages(ids: List<Int>): List<AuctionMainImageProjection>
}