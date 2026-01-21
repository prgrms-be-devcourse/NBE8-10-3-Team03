package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.entity.Chat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Integer> {
    @EntityGraph(attributePaths = {"chatRoom", "chatRoom.post", "chatRoom.auction"})
    List<Chat> findAllByChatRoom_RoomIdOrderByCreateDateAsc(String roomId);

    @EntityGraph(attributePaths = {"chatRoom", "chatRoom.post", "chatRoom.auction"})
    List<Chat> findByChatRoom_RoomIdAndIdGreaterThanOrderByCreateDateAsc(String roomId, int lastId);

    // 읽음 처리 (JPQL)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Chat c SET c.isRead = true WHERE c.chatRoom.roomId = :roomId AND c.sender != :readerName AND c.isRead = false")
    void markMessagesAsRead(@Param("roomId") String roomId,
                            @Param("readerName") String readerName);

    // 참여한 대화의 최신 메세지들 (JPQL)
    @Query("SELECT c FROM Chat c " +
            "WHERE c.id IN (SELECT MAX(c2.id) FROM Chat c2 GROUP BY c2.chatRoom) " +
            "AND ((c.chatRoom.sellerId = :apiKey AND c.chatRoom.sellerExited = false) " +
            "OR (c.chatRoom.buyerId = :apiKey AND c.chatRoom.buyerExited = false))")
    List<Chat> findAllLatestMessagesByMember(@Param("apiKey") String apiKey);
}