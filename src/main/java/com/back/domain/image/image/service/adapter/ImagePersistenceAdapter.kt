package com.back.domain.image.image.service.adapter

import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import com.back.domain.image.image.service.port.ImagePort
import org.springframework.stereotype.Component

@Component
class ImagePersistenceAdapter(
    private val imageRepository: ImageRepository
) : ImagePort {
    override fun save(image: Image): Image {
        return imageRepository.save(image)
    }
    override fun findPostMainImages(ids: List<Int>) =
        imageRepository.findPostMainImages(ids)

    override fun findAuctionMainImages(ids: List<Int>) =
        imageRepository.findAuctionMainImages(ids)
}