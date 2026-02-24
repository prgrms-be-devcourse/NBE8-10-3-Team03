package com.back.domain.auction.auction.service.adapter

import com.back.domain.auction.auction.service.port.AuctionMemberPort
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component

@Component
class AuctionMemberAdapter(
    private val memberService: MemberService,
    private val memberRepository: MemberRepository
) : AuctionMemberPort {
    override fun validateCanCreateAuction(sellerId: Int) {
        if (memberService.findById(sellerId).get().status == MemberStatus.SUSPENDED) {
            throw ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.")
        }
    }

    override fun getSellerOrThrow(sellerId: Int) =
        memberRepository.findById(sellerId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 사용자입니다.") }

    override fun applyCancelPenalty(auctionId: Int, memberId: Int) {
        memberService.decreaseByCancel(auctionId, memberId)
    }
}
