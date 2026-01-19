package com.back.domain.auction.controller;

import com.back.domain.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.dto.response.AuctionIdResponse;
import com.back.domain.auction.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuctionIdResponse> createAuction(
            @Valid @ModelAttribute AuctionCreateRequest request,
            @RequestParam(defaultValue = "1") Integer sellerId // TODO: JWT에서 사용자 ID 추출로 변경 예정
    ) {
        AuctionIdResponse response = auctionService.createAuction(request, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

