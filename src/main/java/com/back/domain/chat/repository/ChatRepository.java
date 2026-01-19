package com.back.domain.chat.repository;

import com.back.domain.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findAllByRoomIdOrderBySendTimeAsc(String roomId);
}