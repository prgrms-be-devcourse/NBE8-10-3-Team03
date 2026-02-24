package com.back.domain.bid.bid.dto.request

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

class BidCreateRequest(
    @field:NotNull(message = "입찰가는 필수입니다.")
    @field:Min(value = 1, message = "입찰가는 1원 이상이어야 합니다.")
    var price: Int? = null
)
