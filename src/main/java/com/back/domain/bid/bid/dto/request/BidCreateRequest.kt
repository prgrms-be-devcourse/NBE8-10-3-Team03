package com.back.domain.bid.bid.dto.request

import jakarta.validation.constraints.Min

data class BidCreateRequest(
    @field:Min(value = 1, message = "입찰가는 1원 이상이어야 합니다.")
    val price: Int
)
