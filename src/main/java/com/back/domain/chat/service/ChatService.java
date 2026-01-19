package com.back.domain.chat.service;

import com.back.domain.chat.dto.ChatDto;
import com.back.domain.chat.entity.Chat;
import com.back.domain.chat.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;

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

    public List<ChatDto> getMessages(String roomId) {
        return chatRepository.findAllByRoomIdOrderByCreateDateAsc(roomId)
                .stream()
                .map(m -> new ChatDto(
                        m.getId(),
                        m.getItemId(),
                        m.getRoomId(),
                        m.getSender(),
                        m.getMessage(),
                        m.getCreateDate(),
                        m.isRead()))
                .collect(Collectors.toList());
    }
}