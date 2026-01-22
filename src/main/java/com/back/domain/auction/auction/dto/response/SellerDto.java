package com.back.domain.auction.auction.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class SellerDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String nickname;
    private Double reputationScore;
}
