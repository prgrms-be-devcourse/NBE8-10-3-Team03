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
import com.back.domain.chat.chat.service.event.ChatMessageCommittedEvent
import com.back.domain.chat.chat.service.event.ChatRoomReadEvent
import com.back.domain.chat.chat.service.port.ChatItemPort
import com.back.domain.chat.chat.service.port.ChatMemberInfo
import com.back.domain.chat.chat.service.port.ChatMemberPort
import com.back.domain.chat.chat.service.port.ChatMediaPort
import com.back.domain.chat.chat.service.port.ChatPersistencePort
import com.back.domain.chat.chat.service.port.ChatPublishPort
import com.back.domain.chat.chat.service.port.ChatUploadFile
import com.back.domain.chat.chat.service.port.ChatUseCase
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatPersistencePort: ChatPersistencePort,
    private val chatItemPort: ChatItemPort,
    private val chatMemberPort: ChatMemberPort,
    private val chatMediaPort: ChatMediaPort,
    private val chatPublishPort: ChatPublishPort,
    private val rq: Rq,
    private val eventPublisher: ApplicationEventPublisher,
    @Value("\${chat.events.async-enabled:true}")
    private val asyncEventsEnabled: Boolean,
) : ChatUseCase {
    private val memberCache = ConcurrentHashMap<Int, CachedCurrentMember>()

    @Transactional
    override fun createChatRoom(itemId: Int, txType: String): RsData<ChatRoomIdResponse> {
        val type = parseTxType(txType)
        val buyer = currentMemberFromDb()

        val createdRoom = when (type) {
            ChatRoomType.POST -> {
                val item = chatItemPort.getPostItemOrThrow(itemId)
                ensureNotSelfTrade(item.sellerApiKey, buyer.apiKey)

                val existingRoom = chatPersistencePort.findExistingRoom(type, itemId, buyer.apiKey)
                val isNew = existingRoom == null
                val room = existingRoom ?: chatPersistencePort.saveRoom(
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

                val existingRoom = chatPersistencePort.findExistingRoom(type, itemId, buyer.apiKey)
                val isNew = existingRoom == null
                val room = existingRoom ?: chatPersistencePort.saveRoom(
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
    override fun saveMessage(req: ChatMessageRequest): RsData<ChatIdResponse> {
        val roomId = req.roomId ?: throw ServiceException("400-1", "채팅방 ID는 필수입니다.")
        val room = findActiveRoom(roomId)

        val sender = currentMemberFromDb()
        requireChatParticipant(room, sender.apiKey)

        val chatMessage = Chat(
            chatRoom = room,
            senderId = sender.id,
            message = req.message,
            read = false,
        )

        val uploadFiles = req.images.orEmpty().map {
            ChatUploadFile(
                filename = it.originalFilename,
                contentType = it.contentType,
                bytes = it.bytes,
            )
        }
        chatMediaPort.saveChatImages(chatMessage, uploadFiles)
        val persistedMessage = chatPersistencePort.saveChat(chatMessage)

        val chatResponse = ChatResponse.from(persistedMessage, sender.profileImageUrl)
        if (asyncEventsEnabled) {
            eventPublisher.publishEvent(
                ChatMessageCommittedEvent(
                    roomId = roomId,
                    senderId = sender.id,
                    senderApiKey = sender.apiKey,
                    senderNickname = sender.nickname,
                    senderProfileImageUrl = sender.profileImageUrl,
                    txType = room.txType,
                    itemId = room.itemId,
                    itemName = room.itemName,
                    itemImageUrl = room.itemImageUrl,
                    itemPrice = room.itemPrice,
                    sellerApiKey = room.sellerApiKey,
                    buyerApiKey = room.buyerApiKey,
                    message = req.message,
                    messageDate = persistedMessage.createDate,
                    roomMessagePayload = chatResponse,
                ),
            )
        } else {
            val opponentApiKey = if (room.sellerApiKey == sender.apiKey) room.buyerApiKey else room.sellerApiKey
            runMessagingSafely(
                onSuccess = { log.info("메시지 브로드캐스팅 성공 - RoomID: {}, MessageID: {}", roomId, persistedMessage.id) },
                onFailure = { e -> log.error("WebSocket 전송 실패 - RoomID: {}, Error: {}", roomId, e.message) },
            ) {
                chatPublishPort.publishRoomMessage(roomId, chatResponse)
            }

            chatMemberPort.findMemberByApiKey(opponentApiKey)?.let { opponent ->
                sendUserNotification(
                    type = "NEW_MESSAGE",
                    room = room,
                    recipient = opponent,
                    opponent = sender,
                    message = req.message,
                    messageDate = persistedMessage.createDate,
                )
            }
        }

        return RsData("200-1", "메시지가 전송되었습니다.", ChatIdResponse(persistedMessage.id))
    }

    @Transactional
    override fun getMessages(roomId: String, lastChatId: Int?): RsData<List<ChatResponse>> {
        val room = findActiveRoom(roomId)

        val me = currentMemberFromDb()
        requireChatParticipant(room, me.apiKey)

        val chats = chatPersistencePort.findRecentChats(roomId, lastChatId)
        val unreadMessageIds = chats.asSequence()
            .filter { !it.read && it.senderId != me.id }
            .map { it.id }
            .toList()
        val unreadMessageIdSet = unreadMessageIds.toSet()

        val updatedCount = if (unreadMessageIds.isEmpty()) 0 else chatPersistencePort.markMessagesAsReadByIds(unreadMessageIds)
        if (updatedCount > 0) {
            if (asyncEventsEnabled) {
                eventPublisher.publishEvent(
                    ChatRoomReadEvent(
                        roomId = roomId,
                        readerId = me.id,
                        updatedCount = updatedCount,
                    ),
                )
            } else {
                val readNotification: Any = mapOf(
                    "readerId" to me.id,
                    "roomId" to roomId,
                )
                chatPublishPort.publishRoomRead(roomId, readNotification)
                log.debug("읽음 알림 전송 - RoomId: {}, ReaderId: {}, UpdatedCount: {}", roomId, me.id, updatedCount)
            }
        }

        val membersByApiKey = chatMemberPort.findMembersByApiKeys(setOf(room.sellerApiKey, room.buyerApiKey))
        val profileByMemberId = membersByApiKey.values.associateBy({ it.id }, { it.profileImageUrl })

        val responses = chats.asReversed().map { chat ->
            val senderProfile = profileByMemberId[chat.senderId]
            ChatResponse.from(
                chat = chat,
                roomId = room.roomId,
                itemId = room.itemId,
                senderProfileImageUrl = senderProfile,
                readOverride = chat.read || unreadMessageIdSet.contains(chat.id),
            )
        }

        return RsData("200-1", "메시지 조회 성공", responses)
    }

    override fun getChatList(): RsData<List<ChatRoomListResponse>> {
        val me = currentMemberFromDb()
        val myApiKey = me.apiKey

        val latestSummaries = chatPersistencePort.findLatestChatSummariesByMember(myApiKey, me.id)
        if (latestSummaries.isEmpty()) {
            return RsData("200-1", "채팅 목록 조회 성공", emptyList())
        }

        val latestChats = chatPersistencePort.findChatsWithRoomsByIds(latestSummaries.map { it.latestChatId })
        if (latestChats.isEmpty()) {
            return RsData("200-1", "채팅 목록 조회 성공", emptyList())
        }

        val unreadCountMap = latestSummaries.associate { it.roomId to it.unreadCount }
        val opponentApiKeys = latestChats.mapNotNull { chat ->
            val room = chat.chatRoom ?: return@mapNotNull null
            if (room.sellerApiKey == myApiKey) room.buyerApiKey else room.sellerApiKey
        }.toSet()

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
    override fun exitChatRoom(roomId: String): RsData<Void?> {
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
            val unreadCount = when (type) {
                "NEW_ROOM" -> 0
                else -> chatPersistencePort.countUnreadMessagesByRoomId(room.roomId, recipient.id)
            }

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
        val now = System.currentTimeMillis()

        memberCache[actor.id]
            ?.takeIf { it.expiresAtMillis > now }
            ?.let { return it.member }

        return chatMemberPort.getMemberOrThrow(actor.id).also { fetched ->
            memberCache[actor.id] = CachedCurrentMember(
                member = fetched,
                expiresAtMillis = now + CURRENT_MEMBER_CACHE_TTL_MILLIS,
            )
        }
    }

    private fun findActiveRoom(roomId: String): ChatRoom =
        chatPersistencePort.findActiveRoom(roomId)
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
        private const val CURRENT_MEMBER_CACHE_TTL_MILLIS = 60_000L
    }

    private data class CachedCurrentMember(
        val member: ChatMemberInfo,
        val expiresAtMillis: Long,
    )
}
