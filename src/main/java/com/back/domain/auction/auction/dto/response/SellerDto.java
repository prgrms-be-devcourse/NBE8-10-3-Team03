package com.back.domain.auction.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SellerDto {
    private Integer id;
    private String nickname;
    private Double reputationScore;
}
