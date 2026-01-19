package com.back.domain.chat.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
public class ChatRoom extends BaseEntity {
    private Long itemId;      // 관련 상품 ID
    private String roomId;    // UUID
    private String sellerId;  // 판매자명
    private String buyerId;   // 구매자명

    public static ChatRoom create(Long itemId, String sellerId, String buyerId) {
        return ChatRoom.builder()
                .itemId(itemId)
                .sellerId(sellerId)
                .buyerId(buyerId)
                .roomId(UUID.randomUUID().toString())
                .build();
    }
}