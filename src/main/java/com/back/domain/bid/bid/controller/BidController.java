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
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    private final Rq rq;
    private final SimpMessagingTemplate messagingTemplate;

    @PostMapping
    public RsData<BidResponse> createBid(
            @PathVariable Integer auctionId,
            @Valid @RequestBody BidCreateRequest request
    ) {
        Member actor = rq.getActor();

        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }

        // 기존 return에 있던 서비스 실행 로직
        RsData<BidResponse> result = bidService.createBid(auctionId, request, actor.getId());

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

