package com.back.domain.auction.auction.controller;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.dto.request.AuctionUpdateRequest;
import com.back.domain.auction.auction.dto.response.AuctionDeleteResponse;
import com.back.domain.auction.auction.dto.response.AuctionDetailResponse;
import com.back.domain.auction.auction.dto.response.AuctionIdResponse;
import com.back.domain.auction.auction.dto.response.AuctionPageResponse;
import com.back.domain.auction.auction.dto.response.AuctionUpdateResponse;
import com.back.domain.auction.auction.service.AuctionService;
import com.back.global.controller.BaseController;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@Tag(name = "경매 관리", description = "경매 물품 등록, 조회, 수정, 삭제 및 낙찰 관련 API")
@RestController
@RequestMapping("/api/v1/auctions")
public class AuctionController extends BaseController {

    private final AuctionService auctionService;

    public AuctionController(Rq rq, AuctionService auctionService) {
        super(rq);
        this.auctionService = auctionService;
    }

    @Operation(
        summary = "경매 물품 등록",
        description = """
            새로운 경매 물품을 등록합니다.
            
            **Content-Type**: multipart/form-data (이미지 업로드 포함)
            
            **필수 필드**:
            - name: 물품명
            - description: 설명
            - startPrice: 시작가
            - categoryId: 카테고리 ID
            - durationHours: 경매 진행 시간(시간 단위)
            
            **선택 필드**:
            - buyNowPrice: 즉시구매가
            - images: 이미지 파일들 (최대 10개)
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "경매 등록 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "201-1",
                      "msg": "경매 물품이 등록되었습니다.",
                      "data": {
                        "auctionId": 123,
                        "message": "경매 물품이 등록되었습니다."
                      }
                    }
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "400-2",
                      "msg": "즉시구매가는 시작가보다 높아야 합니다.",
                      "data": null
                    }
                    """)
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
            responseCode = "404",
            description = "존재하지 않는 카테고리",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "404-2",
                      "msg": "존재하지 않는 카테고리입니다.",
                      "data": null
                    }
                    """)
            )
        )
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<AuctionIdResponse> createAuction(
            @Valid @ModelAttribute AuctionCreateRequest request
    ) {
        return auctionService.createAuction(request, getAuthenticatedMemberId());
    }

    @Operation(summary = "경매 물품 목록 조회", description = "경매 물품 목록을 페이징으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 파라미터",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "400-1",
                      "msg": "페이지 번호는 0 이상이어야 합니다.",
                      "data": null
                    }
                    """)
            )
        )
    })
    @GetMapping
    public RsData<AuctionPageResponse> getAuctions(
            @Parameter(description = "페이지 번호", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "정렬", example = "createdAt,desc") @RequestParam(required = false) String sort,
            @Parameter(description = "카테고리", example = "디지털기기") @RequestParam(required = false) String category,
            @Parameter(description = "상태", example = "OPEN") @RequestParam(required = false) String status
    ) {
        return auctionService.getAuctions(page, size, sort, category, status);
    }

    @Operation(summary = "경매 물품 상세 조회", description = "특정 경매 물품의 상세 정보를 조회합니다.")
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
    @GetMapping("/{auctionId}")
    public RsData<AuctionDetailResponse> getAuctionDetail(
            @Parameter(description = "경매 ID", required = true) @PathVariable Integer auctionId
    ) {
        return auctionService.getAuctionDetail(auctionId);
    }

    @Operation(
        summary = "경매 물품 수정",
        description = """
            경매 물품 정보를 수정합니다.
            
            **Content-Type**: multipart/form-data
            
            **제한사항**: 입찰이 발생한 후에는 수정이 불가능합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "입찰 발생 후 수정 시도",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "400-1",
                      "msg": "입찰이 발생한 경매는 수정할 수 없습니다.",
                      "data": null
                    }
                    """)
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
            description = "권한 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "403-1",
                      "msg": "본인의 경매만 수정할 수 있습니다.",
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
    @PatchMapping(value = "/{auctionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<AuctionUpdateResponse> updateAuction(
            @Parameter(description = "경매 ID", required = true) @PathVariable Integer auctionId,
            @Valid @ModelAttribute AuctionUpdateRequest request
    ) {
        return auctionService.updateAuction(auctionId, request, getAuthenticatedMemberId());
    }

    @Operation(summary = "경매 물품 삭제", description = "경매 물품을 삭제합니다. 입찰이 있는 경우 신용도가 감소합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
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
            description = "권한 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "403-1",
                      "msg": "경매를 삭제할 권한이 없습니다.",
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
    @DeleteMapping("/{auctionId}")
    public RsData<AuctionDeleteResponse> deleteAuction(
            @Parameter(description = "경매 ID", required = true) @PathVariable Integer auctionId
    ) {
        return auctionService.deleteAuction(auctionId, getAuthenticatedMemberId());
    }

    @Operation(summary = "낙찰 거래 취소", description = "낙찰 완료된 거래를 취소합니다. 판매자 또는 낙찰자만 취소할 수 있습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "거래 취소 성공"),
        @ApiResponse(
            responseCode = "400",
            description = "낙찰 완료되지 않은 경매",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "400-1",
                      "msg": "낙찰 완료된 경매만 취소할 수 있습니다.",
                      "data": null
                    }
                    """)
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
            description = "권한 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "resultCode": "403-1",
                      "msg": "거래를 취소할 권한이 없습니다.",
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
    @PostMapping("/{auctionId}/cancel")
    public RsData<Void> cancelTrade(
            @Parameter(description = "경매 ID", required = true) @PathVariable Integer auctionId
    ) {
        return auctionService.cancelTrade(auctionId, getAuthenticatedMemberId());
    }
}

