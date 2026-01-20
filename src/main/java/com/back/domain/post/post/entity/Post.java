package com.back.domain.post.post.entity;

import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Member seller;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private int price;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostStatus status = PostStatus.SALE;

    @Builder.Default
    private boolean deleted = false;


    public void update(String title, String content, int price) {
        this.title = title;
        this.content = content;
        this.price = price;
    }
}