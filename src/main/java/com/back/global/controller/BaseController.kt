package com.back.global.controller

import com.back.domain.member.member.entity.Member
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq

abstract class BaseController(
    protected val rq: Rq,
) {
    protected val authenticatedMember: Member
        get() = runCatching { rq.actor }
            .getOrElse { throw ServiceException("401-1", "로그인이 필요합니다.") }

    protected val authenticatedMemberId: Int
        get() = authenticatedMember.id
}
