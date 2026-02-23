package com.back.domain.auction.auction.service.port

import com.back.domain.member.member.entity.Member

interface AuctionMemberPort {
    fun validateCanCreateAuction(sellerId: Int)
    fun getSellerOrThrow(sellerId: Int): Member
    fun applyCancelPenalty(auctionId: Int, memberId: Int)
}
