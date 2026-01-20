package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.request.ChatMessageRequest;
import com.back.domain.chat.chat.dto.response.ChatIdResponse;
import com.back.domain.chat.chat.dto.response.ChatResponse;
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse;
import com.back.domain.chat.chat.service.ChatService;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/room")
    public RsData<ChatRoomIdResponse> createRoom(@RequestParam int itemId,
                                                 @RequestParam String txType,
                                                 @RequestParam String buyerApiKey) {
        return chatService.createChatRoom(itemId, txType, buyerApiKey);
    }

    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<ChatIdResponse> sendMessage(@Valid @ModelAttribute ChatMessageRequest request) {
        return chatService.saveMessage(request);
    }

    @GetMapping("/room/{roomId}")
    public RsData<List<ChatResponse>> getMessages(
            @PathVariable String roomId,
            @RequestParam(value = "lastChatId", required = false) Integer lastChatId,
            @RequestParam(required = false) String readerName) {
        return chatService.getMessages(roomId, lastChatId, readerName);
    }

    @GetMapping("/list")
    public RsData<List<ChatResponse>> getChatList() {
        return chatService.getChatList();
    }
}