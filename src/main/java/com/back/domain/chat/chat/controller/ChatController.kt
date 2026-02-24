package com.back.domain.chat.chat.controller

import com.back.domain.chat.chat.dto.request.ChatMessageRequest
import com.back.domain.chat.chat.dto.response.ChatIdResponse
import com.back.domain.chat.chat.dto.response.ChatResponse
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse
import com.back.domain.chat.chat.dto.response.ChatRoomListResponse
import com.back.domain.chat.chat.service.ChatService
import com.back.global.rsData.RsData
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping("/room")
    fun createRoom(
        @RequestParam itemId: Int,
        @RequestParam txType: String,
    ): RsData<ChatRoomIdResponse> = chatService.createChatRoom(itemId, txType)

    @PostMapping(value = ["/send"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendMessage(
        @Valid @ModelAttribute request: ChatMessageRequest,
    ): RsData<ChatIdResponse> = chatService.saveMessage(request)

    @GetMapping("/room/{roomId}")
    fun getMessages(
        @PathVariable roomId: String,
        @RequestParam(value = "lastChatId", required = false) lastChatId: Int?,
    ): RsData<List<ChatResponse>> = chatService.getMessages(roomId, lastChatId)

    @GetMapping("/list")
    fun getChatList(): RsData<List<ChatRoomListResponse>> = chatService.chatList

    @PatchMapping("/room/{roomId}/exit")
    fun exitRoom(
        @PathVariable roomId: String,
    ): RsData<Void?> = chatService.exitChatRoom(roomId)
}
