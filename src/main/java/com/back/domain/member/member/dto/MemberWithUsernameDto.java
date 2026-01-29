package com.back.domain.member.member.dto;

import com.back.domain.member.member.entity.Member;

import java.time.LocalDateTime;

public record MemberWithUsernameDto(
        int id,
        LocalDateTime createDate,
        LocalDateTime modifyDate,
        String name,
        String username,
        Double score,
        String profileImgUrl
) {
    public MemberWithUsernameDto(int id, LocalDateTime createDate, LocalDateTime modifyDate, String name, String username, Double score,  String profileImgUrl) {
        this.id = id;
        this.createDate = createDate;
        this.modifyDate = modifyDate;
        this.name = name;
        this.username = username;
        this.score = score;
        this.profileImgUrl = profileImgUrl;
    }

    public MemberWithUsernameDto(Member member) {
        this(
                member.getId(),
                member.getCreateDate(),
                member.getModifyDate(),
                member.getNickname(),
                member.getUsername(),
                member.getReputation().getScore(),
                member.getProfileImgUrl()
        );
    }
}
