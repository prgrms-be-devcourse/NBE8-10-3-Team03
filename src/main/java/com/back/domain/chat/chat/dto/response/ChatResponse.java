package com.back.domain.chat.chat.dto.response;

import com.back.domain.chat.chat.entity.Chat;
import com.back.domain.chat.chat.entity.ChatRoomType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class ChatResponse {
    private final Integer id;
    private final Integer itemId;
    private final String roomId;
    private final Integer senderId;
    @Setter
    private String senderProfileImageUrl;
    private final String message;
    private final LocalDateTime createDate;
    private final List<String> imageUrls;
    private final Boolean read;

    public ChatResponse(Chat chat) {
        this.id = chat.getId();
        this.roomId = chat.getChatRoom().getRoomId();
        this.senderId = chat.getSenderId();
        this.message = chat.getMessage();
        this.createDate = chat.getCreateDate();
        this.read = chat.getRead();

        // ChatImage 엔티티 -> URL String 리스트 변환
        this.imageUrls = chat.getChatImages().stream()
                .map(chatImage -> chatImage.getImage().getUrl())
                .toList();

        // 거래 유형(Post or Auction)에 따른 itemId 추출 로직
        if (chat.getChatRoom().getTxType() == ChatRoomType.POST) {
            this.itemId = (chat.getChatRoom().getPost() != null) ? chat.getChatRoom().getPost().getId() : null;
        } else {
            this.itemId = (chat.getChatRoom().getAuction() != null) ? chat.getChatRoom().getAuction().getId() : null;
        }
    }
}