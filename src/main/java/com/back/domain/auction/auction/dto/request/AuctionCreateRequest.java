package com.back.domain.auction.auction.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuctionCreateRequest {

    @NotBlank(message = "물품 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "물품 설명은 필수입니다.")
    private String description;

    @NotNull(message = "시작가는 필수입니다.")
    @Min(value = 0, message = "시작가는 0원 이상이어야 합니다.")
    private Integer startPrice;

    @Min(value = 0, message = "즉시구매가는 0원 이상이어야 합니다.")
    private Integer buyNowPrice;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Integer categoryId;

    // 경매 지속 시간 (단위: 시간), 기본값 168시간 (7일)
    @Min(value = 1, message = "경매 지속 시간은 최소 1시간 이상이어야 합니다.")
    private Integer durationHours = 168;

    private List<MultipartFile> images;
}

