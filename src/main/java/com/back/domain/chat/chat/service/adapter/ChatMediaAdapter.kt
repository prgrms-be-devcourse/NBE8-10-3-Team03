package com.back.domain.chat.chat.service.adapter

import com.back.domain.auction.auction.service.FileStorageService
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatImage
import com.back.domain.chat.chat.service.port.ChatMediaPort
import com.back.domain.chat.chat.service.port.ChatUploadFile
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * ChatMediaPort 구현체.
 * 업로드 파일을 저장소에 저장하고 Image/ChatImage 관계를 생성한다.
 */
@Component
class ChatMediaAdapter(
    private val fileStorageService: FileStorageService,
    private val imageRepository: ImageRepository,
) : ChatMediaPort {
    /** 비어있지 않은 파일만 처리해 메시지 이미지로 연결한다. */
    override fun saveChatImages(chat: Chat, files: List<ChatUploadFile>) {
        files.filterNot { it.isEmpty }
            .forEach { upload ->
                val multipart = SimpleMultipartFile(upload)
                val imageUrl = fileStorageService.storeFile(multipart)
                val savedImage = imageRepository.save(Image(imageUrl))
                chat.addChatImage(ChatImage(chat, savedImage))
            }
    }

    private class SimpleMultipartFile(private val upload: ChatUploadFile) : MultipartFile {
        override fun getName(): String = "images"
        override fun getOriginalFilename(): String? = upload.filename
        override fun getContentType(): String? = upload.contentType
        override fun isEmpty(): Boolean = upload.isEmpty
        override fun getSize(): Long = upload.bytes.size.toLong()
        override fun getBytes(): ByteArray = upload.bytes
        override fun getInputStream(): InputStream = ByteArrayInputStream(upload.bytes)
        override fun transferTo(dest: java.io.File) {
            dest.writeBytes(upload.bytes)
        }
    }
}
