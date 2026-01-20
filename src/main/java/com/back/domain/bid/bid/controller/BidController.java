package com.back.domain.bid.bid.controller;

import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.dto.response.BidPageResponse;
import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final Rq rq;

    @PostMapping
    public RsData<BidResponse> createBid(
            @PathVariable Integer auctionId,
            @Valid @RequestBody BidCreateRequest request
    ) {
        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        return bidService.createBid(auctionId, request, actor.getId());
    }

    @GetMapping
    public RsData<BidPageResponse> getBids(
            @PathVariable Integer auctionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return bidService.getBids(auctionId, page, size);
    }
}

