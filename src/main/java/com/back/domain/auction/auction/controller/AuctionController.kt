package com.back.domain.auction.auction.controller

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest
import com.back.domain.auction.auction.dto.request.AuctionUpdateRequest
import com.back.domain.auction.auction.dto.response.AuctionDeleteResponse
import com.back.domain.auction.auction.dto.response.AuctionDetailResponse
import com.back.domain.auction.auction.dto.response.AuctionIdResponse
import com.back.domain.auction.auction.dto.response.AuctionPageResponse
import com.back.domain.auction.auction.dto.response.AuctionUpdateResponse
import com.back.domain.auction.auction.service.AuctionService
import com.back.global.controller.BaseController
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "경매 관리", description = "경매 물품 등록, 조회, 수정, 삭제 및 낙찰 관련 API")
@RestController
@RequestMapping("/api/v1/auctions")
class AuctionController(
    rq: Rq,
    private val auctionService: AuctionService
) : BaseController(rq) {

    @Operation(summary = "경매 물품 등록")
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createAuction(@Valid @RequestPart("request") request: AuctionCreateRequest): RsData<AuctionIdResponse> =
        auctionService.createAuction(request, authenticatedMemberId)

    @Operation(summary = "경매 물품 목록 조회")
    @GetMapping
    fun getAuctions(
        @Parameter(description = "페이지 번호", example = "0")
        @RequestParam(defaultValue = "0")
        page: Int,
        @Parameter(description = "페이지 크기", example = "20")
        @RequestParam(defaultValue = "20")
        size: Int,
        @Parameter(description = "정렬", example = "createdAt,desc")
        @RequestParam(required = false)
        sort: String?,
        @Parameter(description = "카테고리", example = "디지털기기")
        @RequestParam(required = false)
        category: String?,
        @Parameter(description = "상태", example = "OPEN")
        @RequestParam(required = false)
        status: String?
    ): RsData<AuctionPageResponse> = auctionService.getAuctions(page, size, sort, category, status)

    @Operation(summary = "경매 물품 상세 조회")
    @GetMapping("/{auctionId}")
    fun getAuctionDetail(
        @Parameter(description = "경매 ID", required = true)
        @PathVariable
        auctionId: Int
    ): RsData<AuctionDetailResponse> {
        val data = auctionService.getAuctionDetailData(auctionId)
        return RsData("200-1", "경매 상세 조회 성공", data)
    }

    @Operation(summary = "경매 물품 수정")
    @PatchMapping(value = ["/{auctionId}"], consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun updateAuction(
        @Parameter(description = "경매 ID", required = true)
        @PathVariable
        auctionId: Int,
        @Valid @RequestPart("request") request: AuctionUpdateRequest
    ): RsData<AuctionUpdateResponse> =
        auctionService.updateAuction(auctionId, request, authenticatedMemberId)

    @Operation(summary = "경매 물품 삭제")
    @DeleteMapping("/{auctionId}")
    fun deleteAuction(
        @Parameter(description = "경매 ID", required = true)
        @PathVariable
        auctionId: Int
    ): RsData<AuctionDeleteResponse> = auctionService.deleteAuction(auctionId, authenticatedMemberId)

    @Operation(summary = "낙찰 거래 취소")
    @PostMapping("/{auctionId}/cancel")
    fun cancelTrade(
        @Parameter(description = "경매 ID", required = true)
        @PathVariable
        auctionId: Int
    ): RsData<Void> = auctionService.cancelTrade(auctionId, authenticatedMemberId)
}
