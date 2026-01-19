package com.back.domain.chat.repository;

import com.back.domain.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Integer> {
    List<Chat> findAllByRoomIdOrderByCreateDateAsc(String roomId);

    List<Chat> findByRoomIdAndIdGreaterThanOrderByCreateDateAsc(String roomId, int lastId);

    // 읽음 처리
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Chat c SET c.isRead = true WHERE c.roomId = :roomId AND c.sender != :readerName AND c.isRead = false")
    void markMessagesAsRead(@Param("roomId") String roomId,
                            @Param("readerName") String readerName);

    // 맨 마지막 대화들
    @Query(value = "SELECT * FROM chat WHERE id IN (SELECT MAX(id) FROM chat GROUP BY room_id) ORDER BY create_date DESC", nativeQuery = true)
    List<Chat> findAllLatestMessages();
}