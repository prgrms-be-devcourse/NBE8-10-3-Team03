package com.back.domain.chat.chat.dto.request

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import org.springframework.web.multipart.MultipartFile

data class ChatMessageRequest(
    @field:NotBlank(message = "채팅방 ID는 필수입니다.")
    var roomId: String? = null,
    var message: String? = null,
    var images: List<MultipartFile>? = null,
) {
    @get:AssertTrue(message = "메시지 내용이 없으면 최소 한 장의 이미지가 필요합니다.")
    val hasMessageOrImage: Boolean
        get() = !message.isNullOrBlank() || !images.isNullOrEmpty()
}
