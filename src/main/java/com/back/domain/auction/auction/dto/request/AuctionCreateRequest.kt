package com.back.domain.auction.auction.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

class AuctionCreateRequest {
    @field:NotBlank(message = "물품 이름은 필수입니다.")
    var name: String? = null

    @field:NotBlank(message = "물품 설명은 필수입니다.")
    var description: String? = null

    @field:NotNull(message = "시작가는 필수입니다.")
    @field:Min(value = 0, message = "시작가는 0원 이상이어야 합니다.")
    var startPrice: Int? = null

    @field:Min(value = 0, message = "즉시구매가는 0원 이상이어야 합니다.")
    var buyNowPrice: Int? = null

    @field:NotNull(message = "카테고리 ID는 필수입니다.")
    var categoryId: Int? = null

    @field:Min(value = 1, message = "경매 지속 시간은 최소 1시간 이상이어야 합니다.")
    var durationHours: Int = 168

    var images: List<MultipartFile>? = null
}
