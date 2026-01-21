package com.back.domain.chat.chat.dto.request;

import jakarta.validation.constraints.AssertTrue;
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

    private String message;

    private List<MultipartFile> images;

    @AssertTrue(message = "메시지 내용이 없으면 최소 한 장의 이미지가 필요합니다.")
    public boolean isMessageOrImageExist() {
        boolean hasMessage = message != null && !message.trim().isEmpty();
        boolean hasImages = images != null && !images.isEmpty();

        return hasMessage || hasImages;
    }
}