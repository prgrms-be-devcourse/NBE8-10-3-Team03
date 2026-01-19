package com.back.domain.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long itemId;
    private String roomId;
    private String sender;
    @Column(columnDefinition = "TEXT")
    private String message;
    private LocalDateTime sendTime;
}