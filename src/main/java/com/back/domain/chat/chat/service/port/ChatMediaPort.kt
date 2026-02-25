package com.back.domain.chat.chat.service.port

import com.back.domain.chat.chat.entity.Chat

/**
 * 채팅 메시지의 첨부 파일 저장을 담당하는 출력 포트.
 * 파일 저장소/이미지 영속화 구현 세부는 어댑터에 위임한다.
 */
interface ChatMediaPort {
    /** 메시지에 포함된 이미지 파일들을 저장하고 Chat 엔티티에 연결한다. */
    fun saveChatImages(chat: Chat, files: List<ChatUploadFile>)
}
