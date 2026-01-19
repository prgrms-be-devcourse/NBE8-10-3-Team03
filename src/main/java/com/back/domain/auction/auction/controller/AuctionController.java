package com.back.domain.auction.auction.controller;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.dto.response.AuctionIdResponse;
import com.back.domain.auction.auction.service.AuctionService;
import com.back.global.rsData.RsData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RsData<AuctionIdResponse> createAuction(
            @Valid @ModelAttribute AuctionCreateRequest request,
            @RequestParam(defaultValue = "1") Integer sellerId // TODO: JWT에서 사용자 ID 추출로 변경 예정
    ) {
        return auctionService.createAuction(request, sellerId);
    }
}

