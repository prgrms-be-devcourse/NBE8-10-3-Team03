package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.entity.Chat;
import com.back.domain.chat.chat.entity.ChatImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatImageRepository extends JpaRepository<ChatImage, Integer> {
    List<ChatImage> findByChat(Chat chat);

    @Modifying
    @Query("DELETE FROM ChatImage ci WHERE ci.chat.chatRoom.roomId = :roomId")
    void deleteAllByRoomId(@Param("roomId") String roomId);
}
