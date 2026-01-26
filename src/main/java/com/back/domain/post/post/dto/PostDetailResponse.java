package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;

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
    @Schema(description = "판매자 등급 뱃지 (안전/우수/일반/주의)", example = "안전한 판매자")
    String sellerBadge,
    @Schema(description = "판매자 실제 신용 점수 (상세에서만 노출)", example = "85.5")
    Double sellerScore,
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
                PostListResponse.calculateBadge(post.getSeller().getReputation().getScore()),
                post.getSeller().getReputation().getScore(),
                post.getPostImages()
                        .stream()
                        .map(pi -> pi.getImage().getUrl())
                        .toList(),
                post.getCreateDate(),
                post.getViewCount()

        );
    }
}