package com.back.domain.chat.service;

import com.back.domain.chat.dto.ChatDto;
import com.back.domain.chat.entity.Chat;
import com.back.domain.chat.entity.ChatRoom;
import com.back.domain.chat.repository.ChatRepository;
import com.back.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;

    @Transactional
    public String createChatRoom(Long itemId, String sellerId, String buyerId) {
        // 이미 존재하는 방인지 확인
        return chatRoomRepository.findByItemIdAndSellerIdAndBuyerId(itemId, sellerId, buyerId)
                .map(ChatRoom::getRoomId)
                .orElseGet(() -> {
                    ChatRoom newRoom = ChatRoom.create(itemId, sellerId, buyerId);
                    return chatRoomRepository.save(newRoom).getRoomId();
                });
    }

    @Transactional
    public void saveMessage(ChatDto chatDto) {
        // 방 존재 여부 검증
        chatRoomRepository.findByRoomId(chatDto.roomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다."));

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