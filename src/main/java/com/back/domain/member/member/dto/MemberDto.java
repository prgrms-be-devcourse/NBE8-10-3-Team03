package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;

import java.time.LocalDateTime;

public record MemberDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String name,
        String profileImgUrl
) {
    public MemberDto(Member member) {
        this(
                member.getId(),
                member.getCreateDate(),
                member.getModifyDate(),
                member.getNickname(),
                member.getProfileImgUrl()
        );
    }
}