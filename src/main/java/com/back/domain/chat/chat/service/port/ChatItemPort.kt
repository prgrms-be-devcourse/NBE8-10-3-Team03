package com.back.domain.chat.chat.service.port

/**
 * 채팅 도메인이 거래 아이템(게시글/경매) 정보를 가져오기 위한 출력 포트.
 * 구현체는 타 도메인 저장소 접근과 검증 규칙을 캡슐화한다.
 */
interface ChatItemPort {
    /** 채팅 시작 가능한 게시글 정보를 조회한다. */
    fun getPostItemOrThrow(postId: Int): ChatItemInfo

    /** 채팅 시작 가능한 경매 정보를 조회한다. */
    fun getAuctionItemOrThrow(auctionId: Int): ChatItemInfo
}

/** 채팅방 생성/알림에 필요한 아이템 스냅샷 데이터. */
data class ChatItemInfo(
    val itemId: Int,
    val itemName: String?,
    val itemPrice: Int?,
    val itemImageUrl: String?,
    val sellerApiKey: String,
)
