package com.back.global.security.adapter

import com.back.domain.member.member.service.MemberService
import com.back.global.security.port.JwtPayloadDecoderPort
import org.springframework.stereotype.Component

@Component
class MemberJwtPayloadDecoder(
    private val memberService: MemberService,
) : JwtPayloadDecoderPort {
    override fun decode(accessToken: String): Map<String, Any>? =
        memberService.payload(accessToken)
}
