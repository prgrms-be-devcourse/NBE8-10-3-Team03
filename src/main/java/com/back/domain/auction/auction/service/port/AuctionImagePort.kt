package com.back.domain.auction.auction.service.port

import com.back.domain.auction.auction.entity.Auction
import org.springframework.web.multipart.MultipartFile

interface AuctionImagePort {
    fun saveImages(auction: Auction, imageFiles: List<MultipartFile>)
    fun replaceImages(auction: Auction, keepImageUrls: List<String>?, newImageFiles: List<MultipartFile>?)
}
