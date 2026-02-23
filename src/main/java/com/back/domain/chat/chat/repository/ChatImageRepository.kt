package com.back.domain.chat.chat.repository

import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatImageRepository : JpaRepository<ChatImage, Int> {
    fun findByChat(chat: Chat?): MutableList<ChatImage?>?

    @Modifying
    @Query("DELETE FROM ChatImage ci WHERE ci.chat.chatRoom.roomId = :roomId")
    fun deleteAllByRoomId(@Param("roomId") roomId: String)
}
