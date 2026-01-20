package com.back.domain.post.post.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PostUpdateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private int price;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Integer categoryId;

    private List<MultipartFile> images;
    private List<String> keepImageUrls;
}