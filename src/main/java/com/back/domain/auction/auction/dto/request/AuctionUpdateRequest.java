package com.back.domain.auction.auction.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AuctionUpdateRequest {

    private String name;

    private String description;

    @Min(value = 0, message = "시작가는 0원 이상이어야 합니다.")
    private Integer startPrice;

    @Min(value = 0, message = "즉시구매가는 0원 이상이어야 합니다.")
    private Integer buyNowPrice;

    private LocalDateTime endAt;

    private List<MultipartFile> images;

    // 기존 이미지 URL 유지 (삭제하지 않을 이미지)
    private List<String> keepImageUrls;
}