package com.back.domain.chat.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {
    private Long itemId;      // 관련 상품 ID
    @Column(unique = true, nullable = false)
    private String roomId;    // UUID
    private String sellerId;  // 판매자명
    private String buyerId;   // 구매자명

    // 현재 클래스의 필드만 빌더로 생성
    @Builder
    private ChatRoom(Long itemId, String roomId, String sellerId, String buyerId) {
        this.itemId = itemId;
        this.roomId = roomId;
        this.sellerId = sellerId;
        this.buyerId = buyerId;
    }

    public static ChatRoom create(Long itemId, String sellerId, String buyerId) {
        return ChatRoom.builder()
                .itemId(itemId)
                .sellerId(sellerId)
                .buyerId(buyerId)
                .roomId(UUID.randomUUID().toString())
                .build();
    }
}