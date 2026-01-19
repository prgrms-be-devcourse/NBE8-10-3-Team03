package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatDto;
import com.back.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/send")
    public void sendMessage(@RequestBody ChatDto chatDto) {
        chatService.saveMessage(chatDto);
    }

    @GetMapping("/room/{roomId}")
    public List<ChatDto> getMessages(@PathVariable String roomId) {
        return chatService.getMessages(roomId);
    }
}