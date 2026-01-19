package com.back.domain.chat.entity;

import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;


@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // 상속관계에서 build 사용
public class Chat extends BaseEntity {
    private Long itemId;
    private String roomId;
    private String sender;
    @Column(columnDefinition = "TEXT")
    private String message;
    private boolean isRead; // 읽음 여부
}