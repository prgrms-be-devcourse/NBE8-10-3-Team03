package com.back.domain.post.post.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PostSaveRequest{

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "물품 설명은 필수입니다.")
    private String content;

    @NotNull(message = "물품 가격은 필수입니다.")
    @Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Integer categoryId;

    private List<MultipartFile> images;
}