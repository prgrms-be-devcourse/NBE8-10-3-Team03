package com.back.domain.member.member.service.port

import com.back.domain.member.member.entity.Member

interface MemberPort {
    fun findById(id: Int): Member? // Nullable로 정의하여 Optional 사용 지양
    fun findByUsername(username: String): Member?
    fun findByNickname(nickname: String): Member?
    fun findByApiKey(apiKey: String): Member?
    fun findAll(): List<Member>
    fun save(member: Member): Member
    fun count(): Long
    // 필요 시 getByIdOrThrow 같은 공통 예외 처리 메서드 추가
    fun getByIdOrThrow(id: Int): Member = findById(id)
        ?: throw com.back.global.exception.ServiceException("404-1", "존재하지 않는 회원입니다.")
}