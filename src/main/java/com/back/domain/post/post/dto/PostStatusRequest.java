package com.back.domain.post.post.dto;

import com.back.domain.post.post.entity.PostStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PostStatusRequest {
    private PostStatus status;
}