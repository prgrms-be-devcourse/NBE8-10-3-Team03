package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
    int id,
    String title,
    String content,
    int price,
    String status,
    String categoryName,
    int sellerId,
    String sellerNickname,
    List<String> imageUrls,
    LocalDateTime createDate,
    long viewCount
) {
    public PostDetailResponse(Post post) {
        this(

                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getPrice(),
                post.getStatus().name(),
                post.getCategory().getName(),
                post.getSeller().getId(),
                post.getSeller().getNickname(),
                post.getPostImages()
                        .stream()
                        .map(pi -> pi.getImage().getUrl())
                        .toList(),
                post.getCreateDate(),
                post.getViewCount()

        );
    }
}