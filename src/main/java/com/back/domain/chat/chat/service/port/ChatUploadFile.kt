package com.back.domain.chat.chat.service.port

/**
 * 프레임워크 타입(MultipartFile) 의존을 피하기 위한 채팅 업로드 파일 모델.
 */
data class ChatUploadFile(
    val filename: String?,
    val contentType: String?,
    val bytes: ByteArray,
) {
    val isEmpty: Boolean
        get() = bytes.isEmpty()
}

