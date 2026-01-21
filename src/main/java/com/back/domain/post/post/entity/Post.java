package com.back.domain.post.post.entity;

import com.back.domain.category.category.entity.Category; // 카테고리 임포트 확인
import com.back.domain.member.member.entity.Member;
import com.back.global.jpa.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostImage> postImages = new ArrayList<>();

    @Column(columnDefinition = "BIGINT DEFAULT 0")
    @Builder.Default
    private long viewCount = 0L;

    public void addPostImage(PostImage postImage) {
        this.postImages.add(postImage);
    }


    public void update(String title, String content, int price, Category category) {
        this.title = title;
        this.content = content;
        this.price = price;
        this.category = category;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void updateStatus(PostStatus status) {
        this.status = status;
    }
}