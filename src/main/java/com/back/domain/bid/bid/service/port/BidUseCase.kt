package com.back.domain.bid.bid.service.port

import com.back.domain.bid.bid.dto.request.BidCreateRequest
import com.back.domain.bid.bid.dto.response.BidPageResponse
import com.back.domain.bid.bid.dto.response.BidResponse
import com.back.global.rsData.RsData

interface BidUseCase {
    fun createBid(auctionId: Int, request: BidCreateRequest, bidderId: Int): RsData<BidResponse>
    fun getBids(auctionId: Int, page: Int, size: Int): RsData<BidPageResponse>
}
