package com.back.domain.bid.bid.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BidCreateRequest {

    @NotNull(message = "입찰가는 필수입니다.")
    @Min(value = 1, message = "입찰가는 1원 이상이어야 합니다.")
    private Integer price;
}

