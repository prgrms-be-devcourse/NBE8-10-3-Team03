package com.back.domain.chat.chat.service

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.auction.auction.service.FileStorageService
import com.back.domain.chat.chat.dto.request.ChatMessageRequest
import com.back.domain.chat.chat.dto.response.ChatIdResponse
import com.back.domain.chat.chat.dto.response.ChatNotification
import com.back.domain.chat.chat.dto.response.ChatResponse
import com.back.domain.chat.chat.dto.response.ChatRoomIdResponse
import com.back.domain.chat.chat.dto.response.ChatRoomListResponse
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.entity.ChatImage
import com.back.domain.chat.chat.entity.ChatRoom
import com.back.domain.chat.chat.entity.ChatRoomType
import com.back.domain.chat.chat.repository.ChatRepository
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.repository.PostRepository
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import org.slf4j.LoggerFactory
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Locale

@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatRepository: ChatRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val postRepository: PostRepository,
    private val auctionRepository: AuctionRepository,
    private val memberRepository: MemberRepository,
    private val imageRepository: ImageRepository,
    private val fileStorageService: FileStorageService,
    private val messagingTemplate: SimpMessagingTemplate,
    private val rq: Rq,
) {

    /**
     * [채팅방 생성 및 입장]
     * @param itemId 게시글 또는 경매 ID
     * @param txType 거래 유형 (POST, AUCTION)
     */
    @Transactional
    fun createChatRoom(itemId: Int, txType: String): RsData<ChatRoomIdResponse> {
        val type = parseTxType(txType) // String -> Enum으로 타입 변환
        val buyer = currentMemberFromDb() // 로그인한 구매자 정보
        val buyerApiKey = buyer.apiKey

        // 거래 유형에 따른 분기
        val createdRoom = when (type) {
            ChatRoomType.POST -> {
                val post = postRepository.findById(itemId)
                    .orElseThrow { ServiceException("404-2", "해당 게시글이 존재하지 않습니다.") }

                // 상품 상태가 판매 중 인지 검증
                if (post.status != PostStatus.SALE) {
                    throw ServiceException("400-1", "판매 중인 상품이 아니므로 채팅을 시작할 수 없습니다.")
                }

                val seller = post.seller
                ensureNotSelfTrade(seller, buyerApiKey)

                // 채팅방 중복 생성 방지
                val existingRoom = chatRoomRepository.findByPostAndBuyerApiKeyAndDeletedFalse(post, buyerApiKey)
                val isNew = existingRoom.isEmpty
                val room = existingRoom.orElseGet { chatRoomRepository.save(ChatRoom.createForPost(post, buyer)) }

                CreatedRoom(room = room, seller = seller, isNew = isNew)
            }

            ChatRoomType.AUCTION -> {
                val auction = auctionRepository.findById(itemId)
                    .orElseThrow { ServiceException("404-3", "존재하지 않는 경매입니다.") }

                val seller = auction.seller
                ensureNotSelfTrade(seller, buyerApiKey)

                val existingRoom = chatRoomRepository.findByAuctionAndBuyerApiKeyAndDeletedFalse(auction, buyerApiKey)
                val isNew = existingRoom.isEmpty
                val room = existingRoom.orElseGet { chatRoomRepository.save(ChatRoom.createForAuction(auction, buyer)) }

                CreatedRoom(room = room, seller = seller, isNew = isNew)
            }
        }

        // 새 채팅방이 생성된 경우 판매자에게 새 채팅방 알림 발송
        if (createdRoom.isNew) {
            sendUserNotification(
                type = "NEW_ROOM",
                room = createdRoom.room,
                recipient = createdRoom.seller,
                opponent = buyer,
                message = null,
                messageDate = LocalDateTime.now(),
            )
        }

        return RsData("200-1", "채팅방에 입장했습니다.", ChatRoomIdResponse(createdRoom.room.roomId))
    }

    /**
     * [메시지 저장 및 전송]
     * 클라이언트가 보낸 메시지를 DB에 저장하고, WebSocket 구독자들에게 브로드캐스팅합니다.
     */
    @Transactional
    fun saveMessage(req: ChatMessageRequest): RsData<ChatIdResponse> {
        val roomId = req.roomId ?: throw ServiceException("400-1", "채팅방 ID는 필수입니다.")
        val room = findActiveRoom(roomId)

        val sender = currentMemberFromDb()
        requireChatParticipant(room, sender.apiKey) // 해당 방 참여자 인지 검증

        // 텍스트 메세지
        val chatMessage = chatRepository.save(
            Chat(
                chatRoom = room,
                senderId = sender.id,
                message = req.message,
                read = false,
            ),
        )

        // 첨부 이미지가 있는 경우
        req.images.orEmpty()
            .filterNot { it.isEmpty }
            .forEach { file ->
                val imageUrl = fileStorageService.storeFile(file)
                val savedImage = imageRepository.save(Image(imageUrl))
                chatMessage.addChatImage(ChatImage(chatMessage, savedImage))
            }

        // 이미지 업데이트
        chatRepository.save(chatMessage)

        // 웹소켓
        val chatResponse = ChatResponse.from(chatMessage, sender.profileImgUrl)
        try {
            messagingTemplate.convertAndSend("/sub/v1/chat/room/$roomId", chatResponse)
            log.info("메시지 브로드캐스팅 성공 - RoomID: {}, MessageID: {}", roomId, chatMessage.id)
        } catch (e: Exception) {
            log.error("WebSocket 전송 실패 - RoomID: {}, Error: {}", roomId, e.message)
        }

        // 상대방에게 알림 전송
        val opponentApiKey = if (room.sellerApiKey == sender.apiKey) room.buyerApiKey else room.sellerApiKey
        memberRepository.findByApiKey(opponentApiKey).orElse(null)?.let { opponent ->
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

    /**
     * [메시지 이력 조회]
     * 특정 채팅방의 메시지 내역을 가져오며, 동시에 '읽음' 처리를 수행합니다.
     * @param lastChatId: 무한 스크롤 구현을 위한 마지막 조회 메시지 ID (null이면 최신 20개)
     */
    @Transactional
    fun getMessages(roomId: String, lastChatId: Int?): RsData<List<ChatResponse>> {
        val room = findActiveRoom(roomId)

        val me = currentMemberFromDb()
        requireChatParticipant(room, me.apiKey)

        // 읽음 상태 업데이트
        val updatedCount = chatRepository.markMessagesAsRead(roomId, me.id)
        if (updatedCount > 0) {
            val readNotification: Any = mapOf(
                "readerId" to me.id,
                "roomId" to roomId,
            )
            messagingTemplate.convertAndSend("/sub/v1/chat/room/$roomId/read", readNotification)
            log.debug("읽음 알림 전송 - RoomId: {}, ReaderId: {}, UpdatedCount: {}", roomId, me.id, updatedCount)
        }

        // 판매자/구매자 정보 조회
        val seller = memberRepository.findByApiKey(room.sellerApiKey).orElse(null)
        val buyer = memberRepository.findByApiKey(room.buyerApiKey).orElse(null)

        // lastChatId 이전의 데이터 20개를 가져옴
        val chats = if (lastChatId == null || lastChatId <= 0) {
            chatRepository.findTop20ByChatRoom_RoomIdOrderByIdDesc(roomId)
        } else {
            chatRepository.findTop20ByChatRoom_RoomIdAndIdLessThanOrderByIdDesc(roomId, lastChatId)
        }

        // 최신순으로 조회된 메시지들을 화면 표시용(과거순)으로 뒤집고 각 메시지마다 발신자가 누구인지 판별하여 프로필 이미지 URL을 매핑
        val responses = chats.asReversed().map { chat ->
            val senderProfile = when (chat.senderId) {
                seller?.id -> seller.profileImgUrl
                buyer?.id -> buyer.profileImgUrl
                else -> null
            }
            ChatResponse.from(chat, senderProfile)
        }

        return RsData("200-1", "메시지 조회 성공", responses)
    }

    /**
     * [나의 채팅 목록 조회]
     * 로그인한 사용자가 참여 중인 모든 채팅방의 최신 상태를 리스트로 반환합니다.
     */
    val chatList: RsData<List<ChatRoomListResponse>>
        get() {
            val me = currentMemberFromDb()
            val myApiKey = me.apiKey

            // 각 방마다의 최신 메시지 하나씩을 가져옴
            val latestChats = chatRepository.findAllLatestChatsByMember(myApiKey)
            if (latestChats.isEmpty()) {
                return RsData("200-1", "채팅 목록 조회 성공", emptyList())
            }

            // 필요한 정보들을 미리 추출하여 벌크 조회 준비
            val roomIds = latestChats.mapNotNull { it.chatRoom?.roomId }.distinct()
            val opponentApiKeys = latestChats.mapNotNull { chat ->
                val room = chat.chatRoom ?: return@mapNotNull null
                if (room.sellerApiKey == myApiKey) room.buyerApiKey else room.sellerApiKey
            }.toSet()

            val postIds = latestChats.mapNotNull { it.chatRoom?.takeIf { room -> room.txType == ChatRoomType.POST }?.post?.id }
            val auctionIds = latestChats.mapNotNull { it.chatRoom?.takeIf { room -> room.txType == ChatRoomType.AUCTION }?.auction?.id }

            // 한꺼번에 조회하여 Map으로 구성
            val unreadCountMap = if (roomIds.isEmpty()) {
                emptyMap()
            } else {
                chatRepository.countUnreadMessagesByRoomIds(roomIds, me.id)
                    .associate { it.roomId to (it.count?.toInt() ?: 0) }
            }

            val opponentMap = memberRepository.findByApiKeyIn(opponentApiKeys).associateBy { it.apiKey }

            val postImageMap = if (postIds.isEmpty()) {
                emptyMap()
            } else {
                imageRepository.findPostMainImages(postIds)
                    .associate { row -> (row[0] as Int) to (row[1] as String) }
            }

            val auctionImageMap = if (auctionIds.isEmpty()) {
                emptyMap()
            } else {
                imageRepository.findAuctionMainImages(auctionIds)
                    .associate { row -> (row[0] as Int) to (row[1] as String) }
            }

            // 수집된 정보를 바탕으로 최종 리스트 응답 객체 생성
            val responseList = latestChats.mapNotNull { chat ->
                val room = chat.chatRoom ?: return@mapNotNull null
                val opponentKey = if (room.sellerApiKey == myApiKey) room.buyerApiKey else room.sellerApiKey
                val opponent = opponentMap[opponentKey] ?: return@mapNotNull null

                val item = resolveListItemInfo(room, postImageMap, auctionImageMap)

                ChatRoomListResponse(
                    roomId = room.roomId,
                    opponentId = opponent.id,
                    opponentNickname = opponent.nickname,
                    opponentProfileImageUrl = opponent.profileImgUrl,
                    opponentReputation = opponent.reputation?.score ?: 50.0,
                    lastMessage = chat.message,
                    lastMessageDate = chat.createDate,
                    unreadCount = unreadCountMap[room.roomId] ?: 0,
                    itemId = item.itemId,
                    itemName = item.itemName,
                    itemImageUrl = item.itemImageUrl,
                    itemPrice = item.itemPrice,
                    txType = room.txType,
                )
            }.sortedByDescending { it.lastMessageDate } // 최신 대화 순 정렬

            log.debug("채팅 목록 조회 완료 - 사용자: {}, 조회된 방 개수: {}", me.nickname, responseList.size)
            return RsData("200-1", "채팅 목록 조회 성공", responseList)
        }

    /**
     * [채팅방 퇴장]
     * 사용자가 방을 나갈 때 호출하며, 두 명 모두 나가면 방을 소프트 딜리트 처리
     */
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

    /**
     * [사용자 알림 전송]
     * WebSocket(STOMP) 채널(/sub/user/{id}/notification)을 통해 특정 사용자에게 실시간 알림 전송
     */
    private fun sendUserNotification(
        type: String,
        room: ChatRoom,
        recipient: Member,
        opponent: Member,
        message: String?,
        messageDate: LocalDateTime?,
    ) {
        try {
            val item = resolveNotificationItemInfo(room) // 알림에 표시할 아이템 정보 추출

            // 알림에 표시할 읽지 않은 총 개수 계산
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
                opponentProfileImageUrl = opponent.profileImgUrl,
                lastMessage = message,
                lastMessageDate = messageDate,
                unreadCount = unreadCount,
                itemId = item.itemId,
                itemName = item.itemName,
                itemImageUrl = item.itemImageUrl,
                itemPrice = item.itemPrice,
                txType = room.txType,
            )

            // 웹소켓 메시지 발행
            messagingTemplate.convertAndSend("/sub/user/${recipient.id}/notification", notification)
            log.info("개인 알림 전송 - Type: {}, Recipient: {}, RoomID: {}", type, recipient.id, room.roomId)
        } catch (e: Exception) {
            log.error("개인 알림 전송 실패 - Recipient: {}, Error: {}", recipient.id, e.message)
        }
    }

    /**
     * 세션 정보(Rq)를 바탕으로 DB에서 최신 회원 정보를 가져옵니다.
     */
    private fun currentMemberFromDb(): Member {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        return memberRepository.findById(actor.id)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 회원입니다.") }
    }

    /**
     * 삭제되지 않은 활성 상태의 채팅방을 조회합니다.
     */
    private fun findActiveRoom(roomId: String): ChatRoom =
        chatRoomRepository.findByRoomIdAndDeletedFalse(roomId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 채팅방입니다.") }

    /**
     * 보안 로직: 로그인한 사용자가 채팅방의 판매자 또는 구매자인지 확인합니다.
     */
    private fun requireChatParticipant(room: ChatRoom, apiKey: String) {
        if (room.sellerApiKey != apiKey && room.buyerApiKey != apiKey) {
            throw ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.")
        }
    }

    /**
     * 비즈니스 로직: 자신의 상품에 채팅을 거는 '셀프 거래'를 금지합니다.
     */
    private fun ensureNotSelfTrade(seller: Member, buyerApiKey: String) {
        if (seller.apiKey == buyerApiKey) {
            throw ServiceException("400-3", "본인의 상품에는 채팅을 개설할 수 없습니다.")
        }
    }

    /**
     * 문자열 거래 타입을 Enum 객체로 변환합니다.
     */
    private fun parseTxType(txType: String): ChatRoomType =
        try {
            ChatRoomType.valueOf(txType.uppercase(Locale.ROOT))
        } catch (_: IllegalArgumentException) {
            throw ServiceException("400-2", "잘못된 거래 유형입니다.")
        }

    /**
     * 리스트 조회 시점에 사용될 아이템 요약 정보를 구성합니다 (in-memory 캐싱된 Map 활용).
     */
    private fun resolveListItemInfo(
        room: ChatRoom,
        postImageMap: Map<Int, String>,
        auctionImageMap: Map<Int, String>,
    ): ItemInfo = when (room.txType) {
        ChatRoomType.POST -> room.post?.let { post ->
            ItemInfo(
                itemId = post.id,
                itemName = post.title,
                itemImageUrl = postImageMap[post.id],
                itemPrice = post.price,
            )
        } ?: ItemInfo()

        ChatRoomType.AUCTION -> room.auction?.let { auction ->
            ItemInfo(
                itemId = auction.id,
                itemName = auction.name,
                itemImageUrl = auctionImageMap[auction.id],
                itemPrice = auction.currentHighestBid ?: auction.startPrice,
            )
        } ?: ItemInfo()
    }

    /**
     * 실시간 알림 발송 시점에 필요한 아이템 상세 정보를 조회합니다.
     */
    private fun resolveNotificationItemInfo(room: ChatRoom): ItemInfo = when (room.txType) {
        ChatRoomType.POST -> room.post?.let { post ->
            val imageUrl = imageRepository.findPostMainImages(listOf(post.id))
                .firstOrNull()
                ?.get(1) as? String

            ItemInfo(
                itemId = post.id,
                itemName = post.title,
                itemImageUrl = imageUrl,
                itemPrice = post.price,
            )
        } ?: ItemInfo()

        ChatRoomType.AUCTION -> room.auction?.let { auction ->
            val imageUrl = imageRepository.findAuctionMainImages(listOf(auction.id))
                .firstOrNull()
                ?.get(1) as? String

            ItemInfo(
                itemId = auction.id,
                itemName = auction.name,
                itemImageUrl = imageUrl,
                itemPrice = auction.currentHighestBid ?: auction.startPrice,
            )
        } ?: ItemInfo()
    }

    /** 생성된 방의 상태 정보를 담는 내부 데이터 클래스 */
    private data class CreatedRoom(
        val room: ChatRoom,
        val seller: Member,
        val isNew: Boolean,
    )

    /** 응답 및 알림용 아이템 공통 정보 데이터 클래스 */
    private data class ItemInfo(
        val itemId: Int? = null,
        val itemName: String? = null,
        val itemImageUrl: String? = null,
        val itemPrice: Int? = null,
    )

    companion object {
        private val log = LoggerFactory.getLogger(ChatService::class.java)
    }
}
