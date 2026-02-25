package com.back.domain.chat.chat.service.adapter

import com.back.domain.auction.auction.service.FileStorageService
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatImage
import com.back.domain.chat.chat.service.port.ChatMediaPort
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

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
    override fun saveChatImages(chat: Chat, files: List<MultipartFile>) {
        files.filterNot { it.isEmpty }
            .forEach { file ->
                val imageUrl = fileStorageService.storeFile(file)
                val savedImage = imageRepository.save(Image(imageUrl))
                chat.addChatImage(ChatImage(chat, savedImage))
            }
    }
}
