package com.back.domain.bid.bid.service.port

import com.back.domain.member.member.entity.Member

interface BidMemberPort {
    fun getBidderOrThrow(bidderId: Int): Member
}
