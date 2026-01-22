package com.back.domain.bid.bid.controller;

import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.dto.response.BidPageResponse;
import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.domain.bid.bid.service.BidService;
import com.back.global.controller.BaseController;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public RsData<BidResponse> createBid(
            @PathVariable Integer auctionId,
            @Valid @RequestBody BidCreateRequest request
    ) {
        RsData<BidResponse> result = bidService.createBid(auctionId, request, getAuthenticatedMemberId());

        if (result.statusCode() == 200) {
            messagingTemplate.convertAndSend("/sub/auctions/" + auctionId, result);
            log.info("실시간 입찰 알림 전송 완료: AuctionId={}, Price={}", auctionId, request.getPrice());
        }

        return result;
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

