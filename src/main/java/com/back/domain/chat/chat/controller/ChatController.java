package com.back.domain.chat.chat.controller;

import com.back.domain.chat.chat.dto.request.ChatMessageRequest;
import com.back.domain.chat.chat.dto.response.ChatIdResponse;
import com.back.domain.chat.chat.dto.response.ChatResponse;
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse;
import com.back.domain.chat.chat.dto.response.ChatRoomListResponse;
import com.back.domain.chat.chat.service.ChatService;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.websocket.Endpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.net.http.WebSocket;
import java.util.List;

@Tag(name = "채팅 관리", description = """
    ## 실시간 1:1 채팅 시스템 API
    
    경매 및 게시글 기반의 채팅방 생성, 메시지 전송, 내역 조회 기능을 제공합니다.
    
    ### WebSocket 실시간 연동 가이드
    이 시스템은 **HTTP(REST API)**와 **WebSocket(STOMP)**이 유기적으로 결합되어 동작합니다.
    
    | 단계 | 프로토콜 | API / Endpoint | 설명 |
    |:---:|:---:|:---|:---|
    | **1** | `HTTP` | **POST** `/api/v1/chat/room` | 채팅방을 생성하거나 기존 방의 `roomId`를 받아옵니다. |
    | **2** | `WS` | **SUBSCRIBE** `/sub/v1/chat/room/{roomId}` | 해당 `roomId`를 구독하여 실시간 메시지를 대기합니다. |
    | **3** | `HTTP` | **POST** `/api/v1/chat/send` | 메시지(텍스트/이미지)를 전송합니다. |
    | **4** | `WS` | **MESSAGE** | 서버가 구독자들에게 메시지를 브로드캐스팅합니다. |
    """)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "채팅방 생성 또는 입장",
            description = """
            특정 상품(경매품 또는 게시글)에 대한 1:1 채팅방을 개설하거나, 이미 존재하는 방의 정보를 반환합니다.
            
            **로직 설명**:
            - **기존 방 존재 시**: DB에 이미 판매자-구매자 간 해당 아이템의 방이 있다면 그 방의 `roomId`를 반환합니다.
            - **신규 생성 시**: 방이 없다면 새로 생성 후 `roomId`를 반환합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "성공 (신규 생성 또는 기존 방 조회)",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "성공", value = """
                    {
                        "resultCode": "200-1",
                        "msg": "채팅방이 생성되었습니다.",
                        "data": { "roomId": "550e8400-e29b-41d4-a716-446655440000" }
                    }
                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "지원하지 않는 거래 유형", value = """
                    {
                        "resultCode": "400-1",
                        "msg": "잘못된 거래 유형입니다. (AUCTION, POST 중 선택)",
                        "data": null
                    }
                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "resultCode": "401-1", "msg": "로그인이 필요합니다.", "data": null }
            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "대상 상품 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "resultCode": "404-1", "msg": "존재하지 않는 경매(또는 게시글)입니다.", "data": null }
            """))
            )
    })
    @PostMapping("/room")
    public RsData<ChatRoomIdResponse> createRoom(
            @Parameter(description = "대상 아이템 ID (경매 ID 또는 게시글 ID)", example = "15", required = true)
            @RequestParam int itemId,
            @Parameter(description = "거래 유형 (반드시 대문자)", example = "AUCTION", schema = @Schema(allowableValues = {"AUCTION", "POST"}), required = true)
            @RequestParam String txType) {
        return chatService.createChatRoom(itemId, txType);
    }

    @Operation(
            summary = "메시지 전송 (텍스트/이미지)",
            description = """
            채팅방에 메시지를 전송합니다. 성공 시 WebSocket을 통해 구독자들에게 실시간 알림이 발송됩니다.
            
            **전송 방식 (Multipart/form-data)**:
            - **`message`**: 텍스트 메시지 (옵션)
            - **`images`**: 이미지 파일 리스트 (옵션, 다중 업로드 가능)
            - **주의**: 메시지와 이미지 중 적어도 하나는 포함되어야 합니다.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "메시지 저장 및 발송 성공",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "성공", value = """
                    {
                        "resultCode": "200-1",
                        "msg": "메시지가 전송되었습니다.",
                        "data": { "chatId": 105 }
                    }
                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효성 검사 실패",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "내용 없음", summary = "메시지와 이미지가 모두 비어있을 때", value = """
                    {
                        "resultCode": "400-1",
                        "msg": "메시지 내용이 없으면 최소 한 장의 이미지가 필요합니다.",
                        "data": null
                    }
                """),
                            @ExampleObject(name = "파일 형식 오류", summary = "이미지가 아닌 파일을 업로드했을 때", value = """
                    {
                        "resultCode": "400-2",
                        "msg": "지원하지 않는 파일 형식입니다.",
                        "data": null
                    }
                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "채팅방 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "resultCode": "404-1", "msg": "존재하지 않는 채팅방입니다.", "data": null }
            """))
            )
    })
    @PostMapping(value = "/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<ChatIdResponse> sendMessage(
            @Valid @ModelAttribute ChatMessageRequest request
    ) {
        return chatService.saveMessage(request);
    }

    @Operation(
            summary = "채팅 내역 조회 (무한 스크롤)",
            description = """
            특정 채팅방의 대화 내역을 조회합니다. **Cursor-based Pagination** 방식을 사용합니다.
            
            **페이징 전략**:
            1. **최초 조회**: `lastChatId` 파라미터 없이 요청 -> 가장 최신 메시지 N개를 반환합니다.
            2. **더 불러오기**: 응답받은 리스트 중 **가장 마지막(가장 오래된) 메시지의 ID**를 `lastChatId`로 요청합니다.
            3. **끝 도달**: 빈 리스트(`[]`)가 반환되면 더 이상 메시지가 없는 것입니다.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "성공_데이터있음", value = """
                    {
                        "resultCode": "200-1",
                        "msg": "메시지 목록 조회 성공",
                        "data": [
                            {
                                "id": 15,
                                "itemId": 10,
                                "roomId": "550e8400...",
                                "senderId": 3,
                                "message": "네 알겠습니다.",
                                "createDate": "2024-02-21T14:05:00",
                                "imageUrls": [],
                                "isRead": true
                            },
                            {
                                "id": 14,
                                "itemId": 10,
                                "roomId": "550e8400...",
                                "senderId": 2,
                                "message": "가격 조정 가능하신가요?",
                                "createDate": "2024-02-21T14:00:00",
                                "imageUrls": ["https://cdn.example.com/img1.jpg"],
                                "isRead": true
                            }
                        ]
                    }
                """),
                            @ExampleObject(name = "성공_데이터없음", summary = "더 이상 불러올 메시지가 없을 때", value = """
                    {
                        "resultCode": "200-1",
                        "msg": "메시지 목록 조회 성공",
                        "data": []
                    }
                """)
                    })
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "resultCode": "403-1", "msg": "해당 채팅방에 접근 권한이 없습니다.", "data": null }
            """))
            )
    })
    @GetMapping("/room/{roomId}")
    public RsData<List<ChatResponse>> getMessages(
            @Parameter(description = "채팅방 UUID", required = true)
            @PathVariable String roomId,
            @Parameter(description = "마지막 메시지 ID (이 값보다 작은 ID를 조회). 최초 조회 시 비워두세요.", example = "100")
            @RequestParam(value = "lastChatId", required = false) Integer lastChatId) {
        return chatService.getMessages(roomId, lastChatId);
    }

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = """
            내가 참여하고 있는 모든 채팅방 목록을 최신순으로 조회합니다.
            
            **포함 정보**:
            - 상대방 정보 (닉네임, 프로필 이미지)
            - 마지막 메시지 내용 및 시간
            - 읽지 않은 메시지 개수 (`unreadCount`)
            - 관련 아이템 정보 (이름, 가격, 이미지, 거래 유형)
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "목록 반환 성공",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "기본 예시", value = """
                    {
                        "resultCode": "200-1",
                        "msg": "채팅 목록 조회 성공",
                        "data": [
                            {
                                "roomId": "550e8400...",
                                "opponentId": 5,
                                "opponentNickname": "거래왕",
                                "opponentProfileImageUrl": "https://k.kakaocdn.net/...",
                                "lastMessage": "내일 2시에 뵐게요",
                                "lastMessageDate": "2024-02-21T15:30:00",
                                "unreadCount": 1,
                                "itemId": 10,
                                "itemName": "아이패드 프로",
                                "itemImageUrl": "https://myshop.com/item.jpg",
                                "itemPrice": 850000,
                                "txType": "AUCTION"
                            }
                        ]
                    }
                """)
                    })
            )
    })
    @GetMapping("/list")
    public RsData<List<ChatRoomListResponse>> getChatList() {
        return chatService.getChatList();
    }

    @Operation(
            summary = "채팅방 나가기",
            description = """
            특정 채팅방에서 퇴장합니다. 상대방에게는 알림이 갈 수도 있습니다(구현 여부에 따름).
            
            **삭제 정책 (Logical Delete)**:
            - **한 명만 나갔을 때**: 나간 사용자의 채팅 목록에서는 사라지지만, 상대방은 여전히 대화 내용을 볼 수 있습니다.
            - **두 명 모두 나갔을 때**: DB에서 채팅방 데이터와 메시지가 **완전히 삭제(Hard Delete)**됩니다.
            """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "퇴장 처리 성공",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "resultCode": "200-1", "msg": "채팅방에서 퇴장하였습니다.", "data": null }
            """))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 방",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "resultCode": "404-1", "msg": "존재하지 않는 채팅방입니다.", "data": null }
            """))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                { "resultCode": "403-1", "msg": "해당 채팅방에 접근 권한이 없습니다.", "data": null }
            """))
            )
    })
    @PatchMapping("/room/{roomId}/exit")
    public RsData<Void> exitRoom(
            @Parameter(description = "채팅방 UUID", required = true)
            @PathVariable String roomId
    ) {
        return chatService.exitChatRoom(roomId);
    }
}