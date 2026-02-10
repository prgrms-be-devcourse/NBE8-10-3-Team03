package com.back.domain.chat.chat.repository;

import com.back.domain.chat.chat.dto.response.UnreadCountResponse;
import com.back.domain.chat.chat.entity.Chat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ChatRepository extends JpaRepository<Chat, Integer> {

    // 읽음 처리 (JPQL) - 업데이트된 행 수 반환
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Chat c SET c.read = true WHERE c.chatRoom.roomId = :roomId AND c.senderId != :readerId AND c.read = false")
    int markMessagesAsRead(@Param("roomId") String roomId, @Param("readerId") Integer readerId);

    // 내 채팅방의 최신 메시지 조회 + 방 정보 + 상품 정보 (JPQL)
    @Query("SELECT c FROM Chat c " +
            "JOIN FETCH c.chatRoom cr " +
            "LEFT JOIN FETCH cr.post p " +
            "LEFT JOIN FETCH cr.auction a " +
            "WHERE c.id IN (" +
            "SELECT MAX(c2.id) FROM Chat c2 " +
            "WHERE c2.chatRoom.deleted = false " +
            "AND ((c2.chatRoom.sellerApiKey = :apiKey AND c2.chatRoom.sellerExited = false) " +
            "OR (c2.chatRoom.buyerApiKey = :apiKey AND c2.chatRoom.buyerExited = false)) " +
            "GROUP BY c2.chatRoom" +
            ") " +
            "ORDER BY c.createDate DESC")
    List<Chat> findAllLatestChatsByMember(@Param("apiKey") String apiKey);

    // 안 읽은 메시지 개수 그룹핑 조회 (리스트로 반환)
    @Query("SELECT new com.back.domain.chat.chat.dto.response.UnreadCountResponse(c.chatRoom.roomId, COUNT(c)) " +
            "FROM Chat c " +
            "WHERE c.chatRoom.roomId IN :roomIds " +
            "AND c.read = false " +
            "AND c.senderId != :memberId " +
            "GROUP BY c.chatRoom.roomId")
    List<UnreadCountResponse> countUnreadMessagesByRoomIds(
            @Param("roomIds") List<String> roomIds,
            @Param("memberId") Integer memberId
    );


    // 최신 메세지 20개만 가져오기 (내림차순)
    @EntityGraph(attributePaths = {"chatRoom"})
    List<Chat> findTop20ByChatRoom_RoomIdOrderByIdDesc(String roomId);

    // 과거 내역을 부르는 경우: lastChatId 보다 작은 메세지 20개 (내림차순)
    @EntityGraph(attributePaths = {"chatRoom"})
    List<Chat> findTop20ByChatRoom_RoomIdAndIdLessThanOrderByIdDesc(String roomId, Integer lastId);

    @Modifying
    @Query("DELETE FROM Chat c WHERE c.chatRoom.roomId = :roomId")
    void deleteAllByRoomId(@Param("roomId") String roomId);
}