package com.back.domain.chat.chat.service

import com.back.domain.chat.chat.dto.request.ChatMessageRequest
import com.back.domain.chat.chat.dto.response.ChatIdResponse
import com.back.domain.chat.chat.dto.response.ChatNotification
import com.back.domain.chat.chat.dto.response.ChatResponse
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse
import com.back.domain.chat.chat.dto.response.ChatRoomListResponse
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.ChatRoomType
import com.back.domain.chat.chat.repository.ChatRepository
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.chat.chat.service.port.ChatItemPort
import com.back.domain.chat.chat.service.port.ChatMemberInfo
import com.back.domain.chat.chat.service.port.ChatMemberPort
import com.back.domain.chat.chat.service.port.ChatMediaPort
import com.back.domain.chat.chat.service.port.ChatPublishPort
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Locale

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatRepository: ChatRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatItemPort: ChatItemPort,
    private val chatMemberPort: ChatMemberPort,
    private val chatMediaPort: ChatMediaPort,
    private val chatPublishPort: ChatPublishPort,
    private val rq: Rq,
) {
    @Transactional
    fun createChatRoom(itemId: Int, txType: String): RsData<ChatRoomIdResponse> {
        val type = parseTxType(txType)
        val buyer = currentMemberFromDb()

        val createdRoom = when (type) {
            ChatRoomType.POST -> {
                val item = chatItemPort.getPostItemOrThrow(itemId)
                ensureNotSelfTrade(item.sellerApiKey, buyer.apiKey)

                val existingRoom = chatRoomRepository.findByTxTypeAndItemIdAndBuyerApiKeyAndDeletedFalse(type, itemId, buyer.apiKey)
                val isNew = existingRoom == null
                val room = existingRoom ?: chatRoomRepository.save(
                    ChatRoom.createForPost(
                        itemId = item.itemId,
                        itemName = item.itemName,
                        itemPrice = item.itemPrice,
                        itemImageUrl = item.itemImageUrl,
                        sellerApiKey = item.sellerApiKey,
                        buyerApiKey = buyer.apiKey,
                    ),
                )
                CreatedRoom(room = room, sellerApiKey = item.sellerApiKey, isNew = isNew)
            }

            ChatRoomType.AUCTION -> {
                val item = chatItemPort.getAuctionItemOrThrow(itemId)
                ensureNotSelfTrade(item.sellerApiKey, buyer.apiKey)

                val existingRoom = chatRoomRepository.findByTxTypeAndItemIdAndBuyerApiKeyAndDeletedFalse(type, itemId, buyer.apiKey)
                val isNew = existingRoom == null
                val room = existingRoom ?: chatRoomRepository.save(
                    ChatRoom.createForAuction(
                        itemId = item.itemId,
                        itemName = item.itemName,
                        itemPrice = item.itemPrice,
                        itemImageUrl = item.itemImageUrl,
                        sellerApiKey = item.sellerApiKey,
                        buyerApiKey = buyer.apiKey,
                    ),
                )
                CreatedRoom(room = room, sellerApiKey = item.sellerApiKey, isNew = isNew)
            }
        }

        if (createdRoom.isNew) {
            val seller = chatMemberPort.findMemberByApiKey(createdRoom.sellerApiKey)
                ?: throw ServiceException("404-1", "존재하지 않는 회원입니다.")

            sendUserNotification(
                type = "NEW_ROOM",
                room = createdRoom.room,
                recipient = seller,
                opponent = buyer,
                message = null,
                messageDate = LocalDateTime.now(),
            )
        }

        return RsData("200-1", "채팅방에 입장했습니다.", ChatRoomIdResponse(createdRoom.room.roomId))
    }

    @Transactional
    fun saveMessage(req: ChatMessageRequest): RsData<ChatIdResponse> {
        val roomId = req.roomId ?: throw ServiceException("400-1", "채팅방 ID는 필수입니다.")
        val room = findActiveRoom(roomId)

        val sender = currentMemberFromDb()
        requireChatParticipant(room, sender.apiKey)

        val chatMessage = chatRepository.save(
            Chat(
                chatRoom = room,
                senderId = sender.id,
                message = req.message,
                read = false,
            ),
        )

        chatMediaPort.saveChatImages(chatMessage, req.images.orEmpty())
        chatRepository.save(chatMessage)

        val chatResponse = ChatResponse.from(chatMessage, sender.profileImageUrl)
        runMessagingSafely(
            onSuccess = { log.info("메시지 브로드캐스팅 성공 - RoomID: {}, MessageID: {}", roomId, chatMessage.id) },
            onFailure = { e -> log.error("WebSocket 전송 실패 - RoomID: {}, Error: {}", roomId, e.message) },
        ) {
            chatPublishPort.publishRoomMessage(roomId, chatResponse)
        }

        val opponentApiKey = if (room.sellerApiKey == sender.apiKey) room.buyerApiKey else room.sellerApiKey
        chatMemberPort.findMemberByApiKey(opponentApiKey)?.let { opponent ->
            sendUserNotification(
                type = "NEW_MESSAGE",
                room = room,
                recipient = opponent,
                opponent = sender,
                message = req.message,
                messageDate = chatMessage.createDate,
            )
        }

        return RsData("200-1", "메시지가 전송되었습니다.", ChatIdResponse(chatMessage.id))
    }

    @Transactional
    fun getMessages(roomId: String, lastChatId: Int?): RsData<List<ChatResponse>> {
        val room = findActiveRoom(roomId)

        val me = currentMemberFromDb()
        requireChatParticipant(room, me.apiKey)

        val updatedCount = chatRepository.markMessagesAsRead(roomId, me.id)
        if (updatedCount > 0) {
            val readNotification: Any = mapOf(
                "readerId" to me.id,
                "roomId" to roomId,
            )
            chatPublishPort.publishRoomRead(roomId, readNotification)
            log.debug("읽음 알림 전송 - RoomId: {}, ReaderId: {}, UpdatedCount: {}", roomId, me.id, updatedCount)
        }

        val seller = chatMemberPort.findMemberByApiKey(room.sellerApiKey)
        val buyer = chatMemberPort.findMemberByApiKey(room.buyerApiKey)

        val chats = if (lastChatId == null || lastChatId <= 0) {
            chatRepository.findTop20ByChatRoom_RoomIdOrderByIdDesc(roomId)
        } else {
            chatRepository.findTop20ByChatRoom_RoomIdAndIdLessThanOrderByIdDesc(roomId, lastChatId)
        }

        val responses = chats.asReversed().map { chat ->
            val senderProfile = when (chat.senderId) {
                seller?.id -> seller.profileImageUrl
                buyer?.id -> buyer.profileImageUrl
                else -> null
            }
            ChatResponse.from(chat, senderProfile)
        }

        return RsData("200-1", "메시지 조회 성공", responses)
    }

    fun getChatList(): RsData<List<ChatRoomListResponse>> {
        val me = currentMemberFromDb()
        val myApiKey = me.apiKey

        val latestChats = chatRepository.findAllLatestChatsByMember(myApiKey)
        if (latestChats.isEmpty()) {
            return RsData("200-1", "채팅 목록 조회 성공", emptyList())
        }

        val roomIds = latestChats.mapNotNull { it.chatRoom?.roomId }.distinct()
        val opponentApiKeys = latestChats.mapNotNull { chat ->
            val room = chat.chatRoom ?: return@mapNotNull null
            if (room.sellerApiKey == myApiKey) room.buyerApiKey else room.sellerApiKey
        }.toSet()

        val unreadCountMap = if (roomIds.isEmpty()) {
            emptyMap()
        } else {
            chatRepository.countUnreadMessagesByRoomIds(roomIds, me.id)
                .associate { it.roomId to (it.count?.toInt() ?: 0) }
        }
        val opponentMap = chatMemberPort.findMembersByApiKeys(opponentApiKeys)

        val responseList = latestChats.mapNotNull { chat ->
            val room = chat.chatRoom ?: return@mapNotNull null
            val opponentKey = if (room.sellerApiKey == myApiKey) room.buyerApiKey else room.sellerApiKey
            val opponent = opponentMap[opponentKey] ?: return@mapNotNull null

            ChatRoomListResponse(
                roomId = room.roomId,
                opponentId = opponent.id,
                opponentNickname = opponent.nickname,
                opponentProfileImageUrl = opponent.profileImageUrl,
                opponentReputation = opponent.reputationScore ?: 50.0,
                lastMessage = chat.message,
                lastMessageDate = chat.createDate,
                unreadCount = unreadCountMap[room.roomId] ?: 0,
                itemId = room.itemId,
                itemName = room.itemName,
                itemImageUrl = room.itemImageUrl,
                itemPrice = room.itemPrice,
                txType = room.txType,
            )
        }.sortedByDescending { it.lastMessageDate }

        log.debug("채팅 목록 조회 완료 - 사용자: {}, 조회된 방 개수: {}", me.nickname, responseList.size)
        return RsData("200-1", "채팅 목록 조회 성공", responseList)
    }

    @Transactional
    fun exitChatRoom(roomId: String): RsData<Void?> {
        val room = findActiveRoom(roomId)
        val me = currentMemberFromDb()

        requireChatParticipant(room, me.apiKey)
        room.exit(me.apiKey)

        if (room.isBothExited) {
            room.softDelete()
        }

        return RsData("200-1", "채팅방에서 퇴장하였습니다.", null)
    }

    private fun sendUserNotification(
        type: String,
        room: ChatRoom,
        recipient: ChatMemberInfo,
        opponent: ChatMemberInfo,
        message: String?,
        messageDate: LocalDateTime?,
    ) {
        runMessagingSafely(
            onSuccess = { log.info("개인 알림 전송 - Type: {}, Recipient: {}, RoomID: {}", type, recipient.id, room.roomId) },
            onFailure = { e -> log.error("개인 알림 전송 실패 - Recipient: {}, Error: {}", recipient.id, e.message) },
        ) {
            val unreadCount = chatRepository
                .countUnreadMessagesByRoomIds(listOf(room.roomId), recipient.id)
                .firstOrNull()
                ?.count
                ?.toInt()
                ?: 0

            val notification = ChatNotification(
                type = type,
                roomId = room.roomId,
                opponentId = opponent.id,
                opponentNickname = opponent.nickname,
                opponentProfileImageUrl = opponent.profileImageUrl,
                lastMessage = message,
                lastMessageDate = messageDate,
                unreadCount = unreadCount,
                itemId = room.itemId,
                itemName = room.itemName,
                itemImageUrl = room.itemImageUrl,
                itemPrice = room.itemPrice,
                txType = room.txType,
            )

            chatPublishPort.publishUserNotification(recipient.id, notification)
        }
    }

    private inline fun runMessagingSafely(
        crossinline onSuccess: () -> Unit,
        crossinline onFailure: (Throwable) -> Unit,
        block: () -> Unit,
    ) {
        runCatching(block)
            .onSuccess { onSuccess() }
            .onFailure(onFailure)
    }

    private fun currentMemberFromDb(): ChatMemberInfo {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        return chatMemberPort.getMemberOrThrow(actor.id)
    }

    private fun findActiveRoom(roomId: String): ChatRoom =
        chatRoomRepository.findByRoomIdAndDeletedFalse(roomId)
            ?: throw ServiceException("404-1", "존재하지 않는 채팅방입니다.")

    private fun requireChatParticipant(room: ChatRoom, apiKey: String) {
        if (room.sellerApiKey != apiKey && room.buyerApiKey != apiKey) {
            throw ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.")
        }
    }

    private fun ensureNotSelfTrade(sellerApiKey: String, buyerApiKey: String) {
        if (sellerApiKey == buyerApiKey) {
            throw ServiceException("400-3", "본인의 상품에는 채팅을 개설할 수 없습니다.")
        }
    }

    private fun parseTxType(txType: String): ChatRoomType =
        try {
            ChatRoomType.valueOf(txType.uppercase(Locale.ROOT))
        } catch (_: IllegalArgumentException) {
            throw ServiceException("400-2", "잘못된 거래 유형입니다.")
        }

    private data class CreatedRoom(
        val room: ChatRoom,
        val sellerApiKey: String,
        val isNew: Boolean,
    )

    companion object {
        private val log = LoggerFactory.getLogger(ChatService::class.java)
    }
}

