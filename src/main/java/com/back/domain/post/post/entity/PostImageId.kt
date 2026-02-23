package com.back.domain.post.post.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class PostImageId implements Serializable {
    private Integer post;
    private Integer image;

    public PostImageId(Integer post, Integer image) {
        this.post = post;
        this.image = image;
    }
}