package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PostDto(
        int id,
        String title,
        String content,
        int price,
        String status,
        Integer categoryId,
        String categoryName,
        List<String> imageUrls,
        LocalDateTime createDate
) {
    public PostDto(Post post) {
        this(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getPrice(),
                post.getStatus().name(),
                post.getCategory() != null ? post.getCategory().getId() : null,
                post.getCategory() != null ? post.getCategory().getName() : null,
                post.getPostImages().stream()
                        .map(postImage -> postImage.getImage().getUrl())
                        .collect(Collectors.toList()),
                post.getCreateDate()
        );
    }
}