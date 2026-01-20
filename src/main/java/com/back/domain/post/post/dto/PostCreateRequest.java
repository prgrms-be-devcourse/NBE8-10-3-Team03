package com.back.domain.post.post.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PostCreateRequest {
    @NotBlank(message = "제목은 필수입니다.")
    private String title;
    @NotBlank(message = "내용은 필수입니다.")
    private String content;
    @Min(0) private int price;
    @NotNull(message = "카테고리를 선택해주세요.")
    private Integer categoryId;
    private List<MultipartFile> images;
}