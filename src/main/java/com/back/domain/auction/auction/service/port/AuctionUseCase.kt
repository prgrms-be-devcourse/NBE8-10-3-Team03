package com.back.domain.auction.auction.service.port

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest
import com.back.domain.auction.auction.dto.request.AuctionUpdateRequest
import com.back.domain.auction.auction.dto.response.AuctionDeleteResponse
import com.back.domain.auction.auction.dto.response.AuctionDetailResponse
import com.back.domain.auction.auction.dto.response.AuctionIdResponse
import com.back.domain.auction.auction.dto.response.AuctionPageResponse
import com.back.domain.auction.auction.dto.response.AuctionSliceResponse
import com.back.domain.auction.auction.dto.response.AuctionUpdateResponse
import com.back.global.rsData.RsData

// Inbound port: 웹/배치 같은 인바운드 어댑터가 의존해야 하는 경매 유스케이스 계약.
interface AuctionUseCase {
    fun createAuction(request: AuctionCreateRequest, sellerId: Int): RsData<AuctionIdResponse>
    fun getAuctions(
        page: Int,
        size: Int,
        sortBy: String?,
        categoryName: String?,
        status: String?
    ): RsData<AuctionPageResponse>

    fun getAuctionDetailData(auctionId: Int): AuctionDetailResponse
    fun updateAuction(auctionId: Int, request: AuctionUpdateRequest, memberId: Int): RsData<AuctionUpdateResponse>
    fun deleteAuction(auctionId: Int, memberId: Int): RsData<AuctionDeleteResponse>
    fun cancelTrade(auctionId: Int, memberId: Int): RsData<Void>
    fun getAuctionsByUserId(
        userId: Int,
        page: Int,
        size: Int,
        sortBy: String?,
        status: String?
    ): RsData<AuctionSliceResponse?>
}
