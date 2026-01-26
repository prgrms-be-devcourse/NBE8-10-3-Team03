package com.back.domain.post.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PostCreateRequest {
    @Schema(description = "게시글 제목 (2자 이상 50자 이하)", example = "아이폰 15 프로 128G 블랙")
    @NotBlank(message = "제목은 필수입니다.")
    @Size(min = 2, max = 50, message = "제목은 2자 이상 50자 이하로 입력해주세요.")
    private String title;

    @Schema(description = "게시글 상세 내용 (10자 이상 1000자 이하)", example = "구매한지 3개월 됐고 상태 좋습니다.")
    @NotBlank(message = "내용은 필수입니다.")
    @Size(min = 10, max = 1000, message = "내용은 10자 이상 1000자 이하로 입력해주세요.")
    private String content;

    @Schema(description = "판매 가격 (0원 이상)", example = "950000")
    @Min(0)
    private int price;

    @Schema(description = "카테고리 ID", example = "1")
    @NotNull(message = "카테고리를 선택해주세요.")
    private Integer categoryId;

    private List<MultipartFile> images;
}