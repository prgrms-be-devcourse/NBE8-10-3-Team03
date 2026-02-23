package com.back.domain.member.member.dto

import com.back.domain.member.member.entity.Member
import java.time.LocalDateTime

@JvmRecord
data class MemberWithUsernameDto(
    val id: Int,
    val createDate: LocalDateTime?,
    val modifyDate: LocalDateTime?,
    val name: String?,
    val username: String?,
    val profileImgUrl: String?,
    val score: Double?
) {
    constructor(member: Member) : this(
        member.id,
        member.createDate,
        member.modifyDate,
        member.nickname,
        member.username,
        member.profileImgUrl,
        member.reputation.score
    )
}