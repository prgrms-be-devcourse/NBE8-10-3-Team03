package com.back.domain.chat.chat.entity;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.member.member.entity.Member;
import com.back.domain.post.post.entity.Post;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {
    @Column(unique = true, nullable = false)
    private String roomId;    // UUID

    // 거래의 종류 (AUCTION or POST)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomType txType;

    // 경매 상품일 경우 참조하고 일반 상품일 경우 NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    // 일반 상품일 경우 참조하고 경매 상품일 경우 NULL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "seller_api_key", nullable = false)
    private String sellerApiKey;

    @Column(name = "buyer_api_key", nullable = false)
    private String buyerApiKey;

    @Column(name = "seller_exited", nullable = false)
    private boolean sellerExited = false;

    @Column(name = "buyer_exited", nullable = false)
    private boolean buyerExited = false;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private ChatRoom(String roomId,
                     ChatRoomType txType,
                     Auction auction,
                     Post post,
                     String sellerApiKey,
                     String buyerApiKey) {
        this.roomId = roomId;
        this.txType = txType;
        this.auction = auction;
        this.post = post;
        this.sellerApiKey = sellerApiKey;
        this.buyerApiKey = buyerApiKey;
    }

    // 경매(Auction) 낙찰 후 채팅방 생성
    public static ChatRoom createForAuction(Auction auction, Member buyer) {
        return ChatRoom.builder()
                .roomId(UUID.randomUUID().toString())
                .txType(ChatRoomType.AUCTION)
                .auction(auction)
                .post(null)
                .sellerApiKey(auction.getSeller().getApiKey())
                .buyerApiKey(buyer.getApiKey())
                .build();
    }

    // 일반 판매(Post)용 채팅방 생성
    public static ChatRoom createForPost(Post post, Member buyer) {
        return ChatRoom.builder()
                .roomId(UUID.randomUUID().toString())
                .txType(ChatRoomType.POST)
                .auction(null)
                .post(post)
                .sellerApiKey(post.getSeller().getApiKey())
                .buyerApiKey(buyer.getApiKey())
                .build();
    }

    // 퇴장
    public void exit(String apiKey) {
        if (this.sellerApiKey.equals(apiKey)) this.sellerExited = true;
        if (this.buyerApiKey.equals(apiKey)) this.buyerExited = true;
    }

    // 둘 다 나갔는지 확인
    public boolean isBothExited() {
        return sellerExited && buyerExited;
    }

    // 소프트 삭제
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}