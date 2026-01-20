package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.ChatDto;
import com.back.domain.chat.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    // 채팅방 생성 및 입장
    @PostMapping("/room")
    public String createRoom(@RequestParam int itemId,
                             @RequestParam String txType,
                             @RequestParam String buyerApiKey) {
        return chatService.createChatRoom(itemId, txType, buyerApiKey);
    }

    @PostMapping("/send")
    public void sendMessage(@RequestBody ChatDto chatDto) {
        chatService.saveMessage(chatDto);
    }

    @GetMapping("/room/{roomId}")
    public List<ChatDto> getMessages(
            @PathVariable String roomId,
            @RequestParam(value = "lastChatId", required = false) Integer lastChatId,
            @RequestParam(required = false) String readerName) {
        return chatService.getMessages(roomId, lastChatId, readerName);
    }

    @GetMapping("/list")
    public List<ChatDto> getChatList() {
        return chatService.getChatList();
    }
}