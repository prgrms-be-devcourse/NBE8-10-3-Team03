package com.back.domain.auction.auction.entity;

public enum AuctionStatus {
    OPEN,       // 진행 중
    CLOSED,     // 종료 (입찰 없음)
    COMPLETED,  // 낙찰 완료
    CANCELLED   // 거래 취소
}

