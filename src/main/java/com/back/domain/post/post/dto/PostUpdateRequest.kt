package com.back.domain.post.post.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

data class PostUpdateRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    var title: String = "",

    @field:NotBlank(message = "내용은 필수입니다.")
    var content: String = "",

    @field:Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    var price: Int = 0,

    @field:NotNull(message = "카테고리를 선택해주세요.")
    var categoryId: Int? = null,

    var images: List<MultipartFile>? = null,
    var keepImageUrls: List<String>? = null
)