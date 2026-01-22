package com.back.domain.auction.auction.controller;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.dto.request.AuctionUpdateRequest;
import com.back.domain.auction.auction.dto.response.AuctionDeleteResponse;
import com.back.domain.auction.auction.dto.response.AuctionDetailResponse;
import com.back.domain.auction.auction.dto.response.AuctionIdResponse;
import com.back.domain.auction.auction.dto.response.AuctionPageResponse;
import com.back.domain.auction.auction.dto.response.AuctionUpdateResponse;
import com.back.domain.auction.auction.service.AuctionService;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;
    private final Rq rq;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<AuctionIdResponse> createAuction(
            @Valid @ModelAttribute AuctionCreateRequest request
    ) {
        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        return auctionService.createAuction(request, actor.getId());
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
        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        return auctionService.updateAuction(auctionId, request, actor.getId());
    }

    @DeleteMapping("/{auctionId}")
    public RsData<AuctionDeleteResponse> deleteAuction(
            @PathVariable Integer auctionId
    ) {
        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        return auctionService.deleteAuction(auctionId, actor.getId());
    }

    @PostMapping("/{auctionId}/cancel")
    public RsData<Void> cancelTrade(
            @PathVariable Integer auctionId
    ) {
        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        return auctionService.cancelTrade(auctionId, actor.getId());
    }
}
