package com.back.domain.chat.service;

import com.back.domain.chat.dto.ChatDto;
import com.back.domain.chat.entity.Chat;
import com.back.domain.chat.repository.ChatRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;

    // 영속성 컨텍스트 관리를 위해 주입
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void saveMessage(ChatDto chatDto) {
        Chat chatMessage = Chat.builder()
                .itemId(chatDto.itemId())
                .roomId(chatDto.roomId())
                .sender(chatDto.sender())
                .message(chatDto.message())
                .isRead(false)
                .build();

        chatRepository.save(chatMessage);
    }

    @Transactional
    public List<ChatDto> getMessages(String roomId, Integer lastChatId, String readerName) {
        if (readerName != null) {
            chatRepository.markMessagesAsRead(roomId, readerName);
        }

        List<Chat> chats;
        if (lastChatId == null || lastChatId <= 0) {
            chats = chatRepository.findAllByRoomIdOrderByCreateDateAsc(roomId);
        } else {
            chats = chatRepository.findByRoomIdAndIdGreaterThanOrderByCreateDateAsc(roomId, lastChatId);
        }

        return chats.stream()
                .map(this::chatDto)
                .collect(Collectors.toList());
    }

    public List<ChatDto> getChatList() {
        return chatRepository.findAllLatestMessages().stream()
                .map(this::chatDto)
                .collect(Collectors.toList());
    }

    private ChatDto chatDto(Chat m) {
        return new ChatDto(
                m.getId(),
                m.getItemId(),
                m.getRoomId(),
                m.getSender(),
                m.getMessage(),
                m.getCreateDate(),
                m.isRead());
    }
}