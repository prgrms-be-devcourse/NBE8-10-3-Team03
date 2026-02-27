package com.back.domain.member.member.service.adapter

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.port.MemberPort
import org.springframework.stereotype.Component

@Component
class MemberPersistenceAdapter (
    private val memberRepository: MemberRepository //
) : MemberPort {
    override fun findById(id: Int): Member? = memberRepository.findById(id).orElse(null)
    override fun findByUsername(username: String): Member? = memberRepository.findByUsername(username).orElse(null)
    override fun findByApiKey(apiKey: String): Member? = memberRepository.findByApiKey(apiKey).orElse(null)
    override fun findAll(): List<Member> = memberRepository.findAll()
    override fun findByNickname(nickname: String): Member? = memberRepository.findByNickname(nickname).orElse(null)
    override fun save(member: Member): Member = memberRepository.save(member)
    override fun count(): Long = memberRepository.count()
}