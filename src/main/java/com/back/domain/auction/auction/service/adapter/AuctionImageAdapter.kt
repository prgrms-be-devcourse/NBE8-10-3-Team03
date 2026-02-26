package com.back.domain.auction.auction.service.adapter

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionImage
import com.back.domain.auction.auction.service.port.AuctionImagePort
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import com.back.global.exception.ServiceException
import com.back.global.storage.port.FileStoragePort
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class AuctionImageAdapter(
    private val fileStoragePort: FileStoragePort,
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
            // 유지 목록이 비어 있으면 기존 이미지를 모두 제거한다.
            auction.clearAuctionImages()
        } else {
            // contains 반복 비용을 줄이기 위해 Set으로 변환한다.
            val keepImageUrlSet = keepImageUrls.toSet()
            auction.auctionImages.removeIf { auctionImage ->
                auctionImage.image.url !in keepImageUrlSet
            }
        }

        newImageFiles.orEmpty().forEach { file ->
            if (file.isEmpty) return@forEach
            persistAuctionImage(auction, file)
        }
    }

    private fun persistAuctionImage(auction: Auction, file: MultipartFile) {
        try {
            val imageUrl = fileStoragePort.storeFile(file, "auction")
            val savedImage = imageRepository.save(Image(imageUrl))
            auction.addAuctionImage(AuctionImage(auction, savedImage))
        } catch (e: Exception) {
            throw ServiceException("500-1", "이미지 저장에 실패했습니다: ${e.message}")
        }
    }
}
