package com.back.domain.auction.auction.dto.request

import jakarta.validation.constraints.Min
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

class AuctionUpdateRequest {
    var name: String? = null
    var description: String? = null

    @field:Min(value = 0, message = "시작가는 0원 이상이어야 합니다.")
    var startPrice: Int? = null

    @field:Min(value = 0, message = "즉시구매가는 0원 이상이어야 합니다.")
    var buyNowPrice: Int? = null

    var endAt: LocalDateTime? = null
    var images: List<MultipartFile>? = null
    var keepImageUrls: List<String>? = null
}
