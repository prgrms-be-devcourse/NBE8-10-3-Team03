package com.back.global.controller;

import com.back.domain.member.member.entity.Member;
import com.back.global.exception.ServiceException;
import com.back.global.rq.Rq;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseController {

    protected final Rq rq;

    /**
     * 현재 로그인한 사용자 조회 (인증 필수)
     * @return 로그인한 Member 객체
     * @throws ServiceException 로그인하지 않은 경우 401 에러
     */
    protected Member getAuthenticatedMember() {
        Member actor = rq.getActor();
        if (actor == null) {
            throw new ServiceException("401-1", "로그인이 필요합니다.");
        }
        return actor;
    }

    /**
     * 현재 로그인한 사용자의 ID 조회 (인증 필수)
     * @return 로그인한 사용자의 ID
     * @throws ServiceException 로그인하지 않은 경우 401 에러
     */
    protected Integer getAuthenticatedMemberId() {
        return getAuthenticatedMember().getId();
    }
}

