package com.back.domain.chat.repository;

import com.back.domain.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {
    // 기존 방이 있는지 확인용
    Optional<ChatRoom> findByItemIdAndSellerIdAndBuyerId(Long itemId, String sellerId, String buyerId);
    Optional<ChatRoom> findByRoomId(String roomId);
}