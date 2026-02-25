package com.back.domain.chat.chat.repository

import com.back.domain.chat.chat.dto.response.UnreadCountResponse
import com.back.domain.chat.chat.entity.Chat
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRepository : JpaRepository<Chat, Int> {
    // 읽음 처리 (JPQL) - 업데이트된 행 수 반환
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Chat c SET c.read = true WHERE c.chatRoom.roomId = :roomId AND c.senderId != :readerId AND c.read = false")
    fun markMessagesAsRead(@Param("roomId") roomId: String, @Param("readerId") readerId: Int): Int

    // 내 채팅방의 최신 메시지 조회 + 방 정보 + 상품 정보 (JPQL)
    @Query(
        "SELECT c FROM Chat c " +
                "JOIN FETCH c.chatRoom cr " +
                "WHERE c.id IN (" +
                "SELECT MAX(c2.id) FROM Chat c2 " +
                "WHERE c2.chatRoom.deleted = false " +
                "AND ((c2.chatRoom.sellerApiKey = :apiKey AND c2.chatRoom.sellerExited = false) " +
                "OR (c2.chatRoom.buyerApiKey = :apiKey AND c2.chatRoom.buyerExited = false)) " +
                "GROUP BY c2.chatRoom" +
                ") " +
                "ORDER BY c.createDate DESC",
    )
    fun findAllLatestChatsByMember(@Param("apiKey") apiKey: String?): MutableList<Chat>

    // 안 읽은 메시지 개수 그룹핑 조회 (리스트로 반환)
    @Query(
        "SELECT new com.back.domain.chat.chat.dto.response.UnreadCountResponse(c.chatRoom.roomId, COUNT(c)) " +
                "FROM Chat c " +
                "WHERE c.chatRoom.roomId IN :roomIds " +
                "AND c.read = false " +
                "AND c.senderId != :memberId " +
                "GROUP BY c.chatRoom.roomId",
    )
    fun countUnreadMessagesByRoomIds(@Param("roomIds") roomIds: List<String>, @Param("memberId") memberId: Int): List<UnreadCountResponse>


    // 최신 메세지 20개만 가져오기 (내림차순)
    @EntityGraph(attributePaths = ["chatRoom"])
    fun findTop20ByChatRoom_RoomIdOrderByIdDesc(roomId: String): List<Chat>

    // 과거 내역을 부르는 경우: lastChatId 보다 작은 메세지 20개 (내림차순)
    @EntityGraph(attributePaths = ["chatRoom"])
    fun findTop20ByChatRoom_RoomIdAndIdLessThanOrderByIdDesc(roomId: String, lastId: Int): List<Chat>

    @Modifying
    @Query("DELETE FROM Chat c WHERE c.chatRoom.roomId = :roomId")
    fun deleteAllByRoomId(@Param("roomId") roomId: String)
}
