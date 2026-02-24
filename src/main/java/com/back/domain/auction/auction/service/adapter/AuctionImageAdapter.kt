package com.back.domain.auction.auction.service.adapter

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionImage
import com.back.domain.auction.auction.service.FileStorageService
import com.back.domain.auction.auction.service.port.AuctionImagePort
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class AuctionImageAdapter(
    private val fileStorageService: FileStorageService,
    private val imageRepository: ImageRepository
) : AuctionImagePort {
    override fun saveImages(auction: Auction, imageFiles: List<MultipartFile>) {
        imageFiles.forEach { file ->
            if (file.isEmpty) return@forEach
            persistAuctionImage(auction, file)
        }
    }

    override fun replaceImages(auction: Auction, keepImageUrls: List<String>?, newImageFiles: List<MultipartFile>?) {
        if (keepImageUrls.isNullOrEmpty()) {
            auction.clearAuctionImages()
        } else {
            auction.auctionImages.removeIf { auctionImage ->
                !keepImageUrls.contains(auctionImage.image.url)
            }
        }

        newImageFiles?.forEach { file ->
            if (file.isEmpty) return@forEach
            persistAuctionImage(auction, file)
        }
    }

    private fun persistAuctionImage(auction: Auction, file: MultipartFile) {
        try {
            val imageUrl = fileStorageService.storeFile(file)
            val savedImage = imageRepository.save(Image(imageUrl))
            auction.addAuctionImage(AuctionImage(auction, savedImage))
        } catch (e: Exception) {
            throw ServiceException("500-1", "이미지 저장에 실패했습니다: ${e.message}")
        }
    }
}
