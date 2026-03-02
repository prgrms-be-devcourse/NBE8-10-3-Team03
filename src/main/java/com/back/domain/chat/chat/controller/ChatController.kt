package com.back.domain.chat.chat.controller

import com.back.domain.chat.chat.dto.request.ChatMessageRequest
import com.back.domain.chat.chat.dto.response.ChatIdResponse
import com.back.domain.chat.chat.dto.response.ChatResponse
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse
import com.back.domain.chat.chat.dto.response.ChatRoomListResponse
import com.back.domain.chat.chat.service.port.ChatUseCase
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "채팅", description = "채팅방 생성, 메시지 전송/조회, 채팅 목록 조회, 채팅방 퇴장 API")
@RestController
@RequestMapping("/api/v1/chat")
class ChatController(
    private val chatService: ChatUseCase,
) {
    @Operation(summary = "채팅방 생성 또는 입장", description = "거래 타입(POST/AUCTION)과 아이템 ID를 기준으로 채팅방을 생성하거나 기존 채팅방에 입장합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "채팅방 입장 성공"),
        ApiResponse(responseCode = "400", description = "잘못된 거래 유형 또는 본인 거래 요청"),
        ApiResponse(responseCode = "401", description = "로그인 필요"),
        ApiResponse(responseCode = "404", description = "게시글/경매를 찾을 수 없음"),
    )
    @PostMapping("/room")
    fun createRoom(
        @Parameter(description = "게시글/경매 ID", required = true, example = "1")
        @RequestParam itemId: Int,
        @Parameter(description = "거래 유형 (POST, AUCTION)", required = true, example = "POST")
        @RequestParam txType: String,
    ): RsData<ChatRoomIdResponse> = chatService.createChatRoom(itemId, txType)

    @Operation(summary = "메시지 전송", description = "텍스트 또는 이미지(복수 가능)를 전송합니다. 메시지/이미지 중 하나는 반드시 필요합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "메시지 전송 성공"),
        ApiResponse(responseCode = "400", description = "요청값 유효성 오류"),
        ApiResponse(responseCode = "401", description = "로그인 필요"),
        ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
        ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
    )
    @PostMapping(value = ["/send"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun sendMessage(
        @Valid @ModelAttribute request: ChatMessageRequest,
    ): RsData<ChatIdResponse> = chatService.saveMessage(request)

    @Operation(summary = "채팅 메시지 조회", description = "채팅방 메시지를 조회합니다. lastChatId를 주면 해당 ID 이전 메시지를 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
        ApiResponse(responseCode = "401", description = "로그인 필요"),
        ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
        ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
    )
    @GetMapping("/room/{roomId}")
    fun getMessages(
        @Parameter(description = "채팅방 UUID", required = true)
        @PathVariable roomId: String,
        @Parameter(description = "커서 기반 페이지네이션 ID(해당 ID 미만 메시지 조회)", example = "100")
        @RequestParam(value = "lastChatId", required = false) lastChatId: Int?,
    ): RsData<List<ChatResponse>> = chatService.getMessages(roomId, lastChatId)

    @Operation(summary = "내 채팅 목록 조회", description = "현재 로그인 사용자가 참여 중인 채팅방 목록을 최신 메시지 순으로 조회합니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "채팅 목록 조회 성공"),
        ApiResponse(responseCode = "401", description = "로그인 필요"),
    )
    @GetMapping("/list")
    fun getChatList(): RsData<List<ChatRoomListResponse>> = chatService.getChatList()

    @Operation(summary = "채팅방 퇴장", description = "채팅방에서 퇴장합니다. 양쪽 모두 퇴장하면 채팅방은 soft delete 처리됩니다.")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "채팅방 퇴장 성공"),
        ApiResponse(responseCode = "401", description = "로그인 필요"),
        ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님"),
        ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
    )
    @PatchMapping("/room/{roomId}/exit")
    fun exitRoom(
        @Parameter(description = "채팅방 UUID", required = true)
        @PathVariable roomId: String,
    ): RsData<Void?> = chatService.exitChatRoom(roomId)
}
