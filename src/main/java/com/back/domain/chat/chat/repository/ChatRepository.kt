package com.back.domain.chat.chat.repository

import com.back.domain.chat.chat.dto.response.UnreadCountResponse
import com.back.domain.chat.chat.entity.Chat
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ChatRepository : JpaRepository<Chat, Int> {
    interface ChatRoomLatestSummaryProjection {
        fun getRoomId(): String
        fun getLatestChatId(): Int
        fun getUnreadCount(): Long?
    }

    // 읽음 처리 (JPQL) - 업데이트된 행 수 반환
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Chat c SET c.read = true WHERE c.chatRoom.roomId = :roomId AND c.senderId != :readerId AND c.read = false")
    fun markMessagesAsRead(@Param("roomId") roomId: String, @Param("readerId") readerId: Int): Int

    // 조회된 메시지 id 집합만 읽음 처리 (방 전체 스캔 방지)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Chat c SET c.read = true WHERE c.id IN :chatIds AND c.read = false")
    fun markMessagesAsReadByIds(@Param("chatIds") chatIds: List<Int>): Int

    // 내 채팅방의 최신 메시지 id만 조회
    @Query(
        "SELECT c.id FROM Chat c " +
                "JOIN c.chatRoom cr " +
                "WHERE cr.deleted = false " +
                "AND ((cr.sellerApiKey = :apiKey AND cr.sellerExited = false) " +
                "OR (cr.buyerApiKey = :apiKey AND cr.buyerExited = false)) " +
                "AND c.id = (" +
                "SELECT MAX(c2.id) FROM Chat c2 WHERE c2.chatRoom.roomId = cr.roomId" +
                ") " +
                "ORDER BY c.createDate DESC",
    )
    fun findLatestChatIdsByMember(@Param("apiKey") apiKey: String): List<Int>

    @Query(
        value = """
            SELECT
                cr.room_id AS roomId,
                MAX(c.id) AS latestChatId,
                SUM(CASE WHEN c.is_read = false AND c.sender_id <> :memberId THEN 1 ELSE 0 END) AS unreadCount
            FROM chat_room cr
            JOIN chat c ON c.room_id = cr.room_id
            WHERE cr.deleted = false
              AND (
                    (cr.seller_api_key = :apiKey AND cr.seller_exited = false)
                 OR (cr.buyer_api_key = :apiKey AND cr.buyer_exited = false)
              )
            GROUP BY cr.room_id
            ORDER BY latestChatId DESC
        """,
        nativeQuery = true,
    )
    fun findLatestChatSummariesByMember(
        @Param("apiKey") apiKey: String,
        @Param("memberId") memberId: Int,
    ): List<ChatRoomLatestSummaryProjection>

    // 테스트/기존 호출 호환용 (서비스 핵심 경로에서는 ID 조회 + 재조회 방식 사용)
    @Query(
        "SELECT c FROM Chat c " +
                "JOIN FETCH c.chatRoom cr " +
                "WHERE c.id IN (" +
                "SELECT MAX(c2.id) FROM Chat c2 " +
                "WHERE c2.chatRoom.deleted = false " +
                "AND ((c2.chatRoom.sellerApiKey = :apiKey AND c2.chatRoom.sellerExited = false) " +
                "OR (c2.chatRoom.buyerApiKey = :apiKey AND c2.chatRoom.buyerExited = false)) " +
                "GROUP BY c2.chatRoom.roomId" +
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

    @Query(
        "SELECT COUNT(c) FROM Chat c " +
                "WHERE c.chatRoom.roomId = :roomId " +
                "AND c.read = false " +
                "AND c.senderId != :memberId",
    )
    fun countUnreadMessagesByRoomId(@Param("roomId") roomId: String, @Param("memberId") memberId: Int): Long


    // 최신 메세지 20개만 가져오기 (내림차순)
    @Query("SELECT c.id FROM Chat c WHERE c.chatRoom.roomId = :roomId ORDER BY c.id DESC")
    fun findTopIdsByRoomId(@Param("roomId") roomId: String, pageable: Pageable): List<Int>

    // 과거 내역을 부르는 경우: lastChatId 보다 작은 메세지 20개 (내림차순)
    @Query("SELECT c.id FROM Chat c WHERE c.chatRoom.roomId = :roomId AND c.id < :lastId ORDER BY c.id DESC")
    fun findTopIdsByRoomIdAndIdLessThan(
        @Param("roomId") roomId: String,
        @Param("lastId") lastId: Int,
        pageable: Pageable,
    ): List<Int>

    @Query(
        "SELECT c FROM Chat c " +
                "WHERE c.id IN :chatIds " +
                "ORDER BY c.id DESC",
    )
    fun findChatsByIds(@Param("chatIds") chatIds: List<Int>): List<Chat>

    @Query(
        "SELECT c FROM Chat c " +
                "JOIN FETCH c.chatRoom cr " +
                "WHERE c.id IN :chatIds " +
                "ORDER BY c.id DESC",
    )
    fun findChatsWithRoomByIds(@Param("chatIds") chatIds: List<Int>): List<Chat>

    @Modifying
    @Query("DELETE FROM Chat c WHERE c.chatRoom.roomId = :roomId")
    fun deleteAllByRoomId(@Param("roomId") roomId: String)
}
