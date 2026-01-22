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
import com.back.global.util.PageUtils;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions")
public class AuctionController extends BaseController {

    private final AuctionService auctionService;

    public AuctionController(Rq rq, AuctionService auctionService) {
        super(rq);
        this.auctionService = auctionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<AuctionIdResponse> createAuction(
            @Valid @ModelAttribute AuctionCreateRequest request
    ) {
        return auctionService.createAuction(request, getAuthenticatedMemberId());
    }

    @GetMapping
    public RsData<AuctionPageResponse> getAuctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        return auctionService.getAuctions(page, size, sort, category, status);
    }

    @GetMapping("/{auctionId}")
    public RsData<AuctionDetailResponse> getAuctionDetail(
            @PathVariable Integer auctionId
    ) {
        return auctionService.getAuctionDetail(auctionId);
    }

    @PatchMapping(value = "/{auctionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<AuctionUpdateResponse> updateAuction(
            @PathVariable Integer auctionId,
            @Valid @ModelAttribute AuctionUpdateRequest request
    ) {
        return auctionService.updateAuction(auctionId, request, getAuthenticatedMemberId());
    }

    @DeleteMapping("/{auctionId}")
    public RsData<AuctionDeleteResponse> deleteAuction(
            @PathVariable Integer auctionId
    ) {
        return auctionService.deleteAuction(auctionId, getAuthenticatedMemberId());
    }

    @PostMapping("/{auctionId}/cancel")
    public RsData<Void> cancelTrade(
            @PathVariable Integer auctionId
    ) {
        return auctionService.cancelTrade(auctionId, getAuthenticatedMemberId());
    }
}
