package com.back.domain.search.search.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnifiedSearchResponse {
    private int id;
    private String type;         // "POST" 또는 "AUCTION"
    private String title;        // 공통 제목 (Post: title / Auction: name)
    private int price;           // 공통 가격
    private String status;       // 공통 상태
    private String statusDisplayName;
    private String categoryName; // 공통 카테고리명
    private String thumbnailUrl; // 공통 대표 이미지
    private LocalDateTime createDate;
    private long viewCount;
    private int sellerId;
    private String sellerNickname;
    private String sellerBadge;
}