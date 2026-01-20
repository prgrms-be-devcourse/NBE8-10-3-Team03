package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;
import java.time.LocalDateTime;

public record PostDto(
        int id,
        String title,
        String content,
        int price,
        String status,
        LocalDateTime createDate
) {
    public PostDto(Post post) {
        this(post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getPrice(),
                post.getStatus().name(),
                post.getCreateDate());
    }
}