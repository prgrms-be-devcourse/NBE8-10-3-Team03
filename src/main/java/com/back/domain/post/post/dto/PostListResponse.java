package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;
import java.time.LocalDateTime;

public record PostListResponse(
    int id,
    String title,
    int price,
    String categoryName,
    String thumbnailUrl,
    LocalDateTime createDate,
    String status,
    String statusDisplayName,
    long viewCount,
    int sellerId,
    String sellerNickname
) {
    public PostListResponse(Post post) {
        this(
                post.getId(),
                post.getTitle(),
                post.getPrice(),
                post.getCategory().getName(),
                post.getPostImages().isEmpty() ? null
                        : post.getPostImages().get(0).getImage().getUrl(),
                post.getCreateDate(),
                post.getStatus().name(),
                post.getStatus().getDisplayName(),
                post.getViewCount(),
                post.getSeller().getId(),
                post.getSeller().getNickname()
        );
    }
}