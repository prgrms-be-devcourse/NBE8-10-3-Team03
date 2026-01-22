package com.back.domain.bid.bid.controller;

import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.dto.response.BidPageResponse;
import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.domain.bid.bid.service.BidService;
import com.back.global.controller.BaseController;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.websocket.Endpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.http.WebSocket;

@Tag(name = "입찰 관리", description = """
    ## 경매 물품에 대한 입찰 등록 및 조회 API
    
    경매 물품에 대한 입찰 등록, 조회 및 **WebSocket 실시간 가격 갱신**을 담당합니다.
    
    ### WebSocket 실시간 연동 가이드
    입찰이 발생하면 서버는 해당 경매를 보고 있는 모든 사용자에게 새로운 가격 정보를 브로드캐스팅합니다.
    
    | 단계 | 프로토콜 | API / Endpoint | 설명 |
    |:---:|:---:|:---|:---|
    | **1** | `WS` | **SUBSCRIBE** `/sub/v1/auctions/{auctionId}` | 경매 상세 페이지 진입 시 해당 경매 ID를 구독합니다. |
    | **2** | `HTTP` | **POST** `/api/v1/auctions/{auctionId}/bids` | 사용자가 새로운 입찰 요청을 보냅니다. |
    | **3** | `WS` | **MESSAGE** | 입찰 성공 시, 구독 중인 모든 클라이언트에게 `BidResponse` JSON이 전송됩니다. |
    """)
@Slf4j
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
public class BidController extends BaseController {

    private final BidService bidService;
    private final SimpMessagingTemplate messagingTemplate;

    public BidController(Rq rq, BidService bidService, SimpMessagingTemplate messagingTemplate) {
        super(rq);
        this.bidService = bidService;
        this.messagingTemplate = messagingTemplate;
    }

    @Operation(
        summary = "입찰하기",
        description = """
            경매 물품에 입찰합니다.
            
            **입찰 규칙**:
            - 현재 최고가보다 높아야 합니다
            - 현재가의 150%를 초과할 수 없습니다
            - 즉시구매가를 초과할 수 없습니다
            - 자신의 경매에는 입찰할 수 없습니다
            - 이미 최고가 입찰자인 경우 연속 입찰 불가
            - **즉시구매가와 동일한 금액 입찰 시 즉시 낙찰됩니다**
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "입찰 성공 또는 즉시구매 성공 (실시간 알림 발송됨)"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "입찰 규칙 위반",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "진행 중인 경매 아님",
                        value = """
                            {
                              "resultCode": "400-1",
                              "msg": "진행 중인 경매가 아닙니다.",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "경매 종료",
                        value = """
                            {
                              "resultCode": "400-2",
                              "msg": "종료된 경매입니다.",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "입찰가 부족",
                        value = """
                            {
                              "resultCode": "400-3",
                              "msg": "입찰가는 현재 최고가(200,000원)보다 높아야 합니다.",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "입찰가 초과",
                        value = """
                            {
                              "resultCode": "400-4",
                              "msg": "입찰가는 현재가의 150% (300,000원)를 초과할 수 없습니다.",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "즉시구매가 초과",
                        value = """
                            {
                              "resultCode": "400-5",
                              "msg": "입찰가는 즉시구매가(250,000원)를 초과할 수 없습니다.",
                              "data": null
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "연속 입찰 시도",
                        value = """
                            {
                              "resultCode": "400-6",
                              "msg": "이미 최고가 입찰자입니다. 다른 입찰자가 입찰할 때까지 기다려주세요.",
                              "data": null
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "401-1",
                      "msg": "로그인이 필요합니다.",
                      "data": null
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "403",
            description = "본인 경매 입찰 시도",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "403-1",
                      "msg": "자신의 경매에는 입찰할 수 없습니다.",
                      "data": null
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 경매",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "404-1",
                      "msg": "존재하지 않는 경매입니다.",
                      "data": null
                    }
                    """)
            )
        )
    })
    @PostMapping
    public RsData<BidResponse> createBid(
            @Parameter(description = "경매 ID", required = true) @PathVariable Integer auctionId,
            @Valid @RequestBody BidCreateRequest request
    ) {
        RsData<BidResponse> result = bidService.createBid(auctionId, request, getAuthenticatedMemberId());

        if (result.statusCode() == 200) {
            messagingTemplate.convertAndSend("/sub/v1/auctions/" + auctionId, result);
            log.info("실시간 입찰 알림 전송 완료: AuctionId={}, Price={}", auctionId, request.getPrice());
        }

        return result;
    }

    @Operation(summary = "경매 입찰 내역 조회", description = "특정 경매에 대한 입찰 내역을 최신순으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 경매",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "404-1",
                      "msg": "존재하지 않는 경매입니다.",
                      "data": null
                    }
                    """)
            )
        )
    })
    @GetMapping
    public RsData<BidPageResponse> getBids(
            @Parameter(description = "경매 ID", required = true) @PathVariable Integer auctionId,
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size
    ) {
        return bidService.getBids(auctionId, page, size);
    }
}

