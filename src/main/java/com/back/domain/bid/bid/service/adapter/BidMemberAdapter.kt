package com.back.domain.bid.bid.service.adapter

import com.back.domain.bid.bid.service.port.BidMemberPort
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component

@Component
class BidMemberAdapter(
    private val memberRepository: MemberRepository
) : BidMemberPort {
    override fun getBidderOrThrow(bidderId: Int): Member =
        memberRepository.findById(bidderId)
            .orElseThrow { ServiceException("404-2", "존재하지 않는 사용자입니다.") }
}
