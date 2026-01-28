package com.back.domain.auction.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SellerDto {
    private Integer id;
    private String nickname;
    private Double reputationScore;
}
