package com.back.domain.chat.chat.entity;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@EqualsAndHashCode
public class ChatImageId implements Serializable {
    private Integer chat;
    private Integer image;

    public ChatImageId(Integer chat, Integer image) {
        this.chat = chat;
        this.image = image;
    }
}
