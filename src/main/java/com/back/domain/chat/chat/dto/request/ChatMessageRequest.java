package com.back.domain.chat.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRequest {

    @NotBlank(message = "채팅방 ID는 필수입니다.")
    private String roomId;

    @NotBlank(message = "보내는 사람 이름은 필수입니다.")
    private String sender;

    private String message;

    private List<MultipartFile> images;
}