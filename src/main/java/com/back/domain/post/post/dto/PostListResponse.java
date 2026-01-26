package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;

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
    String sellerNickname,
    @Schema(description = "판매자 등급 뱃지 (안전/우수/일반/주의)", example = "안전한 판매자")
    String sellerBadge
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
                post.getSeller().getNickname(),
                calculateBadge(post.getSeller().getReputation() != null ?
                        post.getSeller().getReputation().getScore() : null)
        );
    }

    public static String calculateBadge(Double score) {
        if (score == null) return "일반 판매자";
        if (score >= 80) return "안전한 판매자";
        if (score >= 60) return "우수 판매자";
        if (score >= 40) return "일반 판매자";
        return "주의 판매자";
    }
}