package com.back.domain.auction.auction.service.port

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionImage
import org.springframework.web.multipart.MultipartFile

interface AuctionImagePort {
    fun saveImages(auction: Auction, imageFiles: List<MultipartFile>) : List<AuctionImage>
    fun replaceImages(auction: Auction, keepImageUrls: List<String>?, newImageFiles: List<MultipartFile>?)
}
