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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
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
    @Qualifier("chatTaskExecutor")
    private val chatTaskExecutor: Executor,
    private val rq: Rq,
) {

    /**
     * [채팅방 생성 및 입장]
     * @param itemId 게시글 또는 경매 ID
     * @param txType 거래 유형 (POST, AUCTION)
     * - 기존 방이 있다면 해당 방 ID를 반환하고, 없다면 새로 생성합니다.
     * - 새로운 방이 생성될 때만 판매자에게 웹소켓 실시간 알림을 보냅니다.
     */
    @Transactional
    fun createChatRoom(itemId: Int, txType: String): RsData<ChatRoomIdResponse> {
        val type = parseTxType(txType) // String 에서 Enum으로 타입 변환
        val buyer = currentMemberFromDb() // 현재 로그인한 구매자 정보
        val buyerApiKey = requireApiKey(buyer)

        val createdRoom = when (type) {
            ChatRoomType.POST -> {
                val post = postRepository.findById(itemId)
                    .orElseThrow { ServiceException("404-2", "해당 게시글이 존재하지 않습니다.") }

                // 판매 중인 상품에 대해서만 채팅이 가능함
                if (post.status != PostStatus.SALE) {
                    throw ServiceException("400-1", "판매 중인 상품이 아니므로 채팅을 시작할 수 없습니다.")
                }

                val seller = post.seller
                ensureNotSelfTrade(seller, buyerApiKey) // 본인 상품 채팅 제한

                // 기존 방 존재 여부 확인 (Soft Delete 되지 않은 방 기준)
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

        // 방이 새로 생성된 경우에만 판매자에게 '새 방 개설' 실시간 알림 전송
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
     * - 텍스트 및 이미지 메시지를 DB에 영구 저장합니다.
     * - 웹소켓을 통해 같은 방(/sub/v1/chat/room/{id})에 접속한 유저에게 브로드캐스팅합니다.
     * - 상대방이 방 밖에 있을 경우를 대비해 개인 알림 채널로 알람을 보냅니다.
     */
    @Transactional
    fun saveMessage(req: ChatMessageRequest): RsData<ChatIdResponse> {
        val roomId = req.roomId ?: throw ServiceException("400-1", "채팅방 ID는 필수입니다.")
        val room = findActiveRoom(roomId)

        val sender = currentMemberFromDb()
        val senderApiKey = requireApiKey(sender)
        requireChatParticipant(room, senderApiKey) // 권한 검증: 방 참여자만 메시지 전송 가능

        // 메시지 엔티티 생성 및 기본 텍스트 저장
        val chatMessage = chatRepository.save(
            Chat(
                chatRoom = room,
                senderId = sender.id,
                message = req.message,
                read = false,
            ),
        )

        // 이미지 파일을 저장소에 올리고 DB 관계를 맺음
        req.images.orEmpty()
            .filterNot { it.isEmpty }
            .forEach { file ->
                val imageUrl = fileStorageService.storeFile(file)
                val savedImage = imageRepository.save(Image(imageUrl))
                chatMessage.addChatImage(ChatImage(chatMessage, savedImage))
            }

        chatRepository.save(chatMessage)

        // 웹소켓 실시간 브로드캐스팅
        val chatResponse = ChatResponse.from(chatMessage, sender.profileImgUrl)

        runMessagingSafely(
            onSuccess = { log.info("메시지 브로드캐스팅 성공 - RoomID: {}, MessageID: {}", roomId, chatMessage.id) },
            onFailure = { e -> log.error("WebSocket 전송 실패 - RoomID: {}, Error: {}", roomId, e.message) },
        ) {
            messagingTemplate.convertAndSend("/sub/v1/chat/room/$roomId", chatResponse)
        }

        // 상대방에게 개인 채널로 실시간 알림 전송
        val opponentApiKey = if (room.sellerApiKey == senderApiKey) room.buyerApiKey else room.sellerApiKey
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
     * - 채팅방 진입 시 메시지 내역을 불러오며, 읽지 않은 상대방의 메시지를 '읽음' 처리합니다.
     * - 읽음 처리 성공 시, 상대방 화면의 읽음 표시를 업데이트하도록 실시간 알림을 보냅니다.
     * @param lastChatId 무한스크롤용 커서 (null이면 최신 20개, 값이 있으면 해당 ID보다 작은 메시지 20개 조회)
     */
    @Transactional
    fun getMessages(roomId: String, lastChatId: Int?): RsData<List<ChatResponse>> {
        val room = findActiveRoom(roomId)

        val me = currentMemberFromDb()
        requireChatParticipant(room, requireApiKey(me))

        // 상대방이 보낸 메시지들을 읽음 처리
        val updatedCount = chatRepository.markMessagesAsRead(roomId, me.id)
        if (updatedCount > 0) {
            // 읽음 처리가 발생했다면, 상대방에게 내 읽음 상태를 알림
            val readNotification: Any = mapOf(
                "readerId" to me.id,
                "roomId" to roomId,
            )
            messagingTemplate.convertAndSend("/sub/v1/chat/room/$roomId/read", readNotification)
            log.debug("읽음 알림 전송 - RoomId: {}, ReaderId: {}, UpdatedCount: {}", roomId, me.id, updatedCount)
        }

        // 발신자 프로필 이미지 매핑을 위한 정보 획득
        val seller = memberRepository.findByApiKey(room.sellerApiKey).orElse(null)
        val buyer = memberRepository.findByApiKey(room.buyerApiKey).orElse(null)

        // No-Offset 페이징 조회: 최신순(Desc)으로 20개 가져오기
        val chats = if (lastChatId == null || lastChatId <= 0) {
            chatRepository.findTop20ByChatRoom_RoomIdOrderByIdDesc(roomId)
        } else {
            chatRepository.findTop20ByChatRoom_RoomIdAndIdLessThanOrderByIdDesc(roomId, lastChatId)
        }

        // 사용자 화면(위에서 아래로 흐르는 시간순) 구성을 위해 리스트를 반전하고 DTO로 변환
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
     * - 사용자가 속한 모든 채팅방의 목록을 가져옵니다.
     * - 각 방의 최신 메시지, 안 읽은 메시지 수, 거래 물건 정보를 포함합니다.
     * - N+1 성능 이슈를 방지하기 위해 데이터를 메모리(Map)에 적재하여 조인 없이 매핑합니다.
     */
    val chatList: RsData<List<ChatRoomListResponse>>
        get() {
            val me = currentMemberFromDb()
            val myApiKey = me.apiKey

            // 내가 속한 각 방들의 최신 메시지 1개씩 조회
            val latestChats = chatRepository.findAllLatestChatsByMember(myApiKey)
            if (latestChats.isEmpty()) {
                return RsData("200-1", "채팅 목록 조회 성공", emptyList())
            }

            // 일괄 조회를 위해 ID 및 API Key 추출 (in-memory 캐싱 맵 구성용)
            val roomIds = latestChats.mapNotNull { it.chatRoom?.roomId }.distinct()
            val opponentApiKeys = latestChats.mapNotNull { chat ->
                val room = chat.chatRoom ?: return@mapNotNull null
                if (room.sellerApiKey == myApiKey) room.buyerApiKey else room.sellerApiKey
            }.toSet()

            val postIds = latestChats.mapNotNull { it.chatRoom?.takeIf { room -> room.txType == ChatRoomType.POST }?.post?.id }
            val auctionIds = latestChats.mapNotNull { it.chatRoom?.takeIf { room -> room.txType == ChatRoomType.AUCTION }?.auction?.id }

            // AsyncConfig(chatTaskExecutor) 기반 병렬 조회
            val unreadFuture = CompletableFuture.supplyAsync(
                {
                    if (roomIds.isEmpty()) {
                        emptyMap<String, Int>()
                    } else {
                        chatRepository.countUnreadMessagesByRoomIds(roomIds, me.id)
                            .associate { it.roomId to (it.count?.toInt() ?: 0) }
                    }
                },
                chatTaskExecutor,
            )

            val opponentFuture = CompletableFuture.supplyAsync(
                { memberRepository.findByApiKeyIn(opponentApiKeys.toMutableSet()).associateBy { it.apiKey } },
                chatTaskExecutor,
            )

            val postImageFuture = CompletableFuture.supplyAsync(
                {
                    if (postIds.isEmpty()) {
                        emptyMap<Int, String>()
                    } else {
                        imageRepository.findPostMainImages(postIds)
                            .associate { row -> (row[0] as Int) to (row[1] as String) }
                    }
                },
                chatTaskExecutor,
            )

            val auctionImageFuture = CompletableFuture.supplyAsync(
                {
                    if (auctionIds.isEmpty()) {
                        emptyMap<Int, String>()
                    } else {
                        imageRepository.findAuctionMainImages(auctionIds)
                            .associate { row -> (row[0] as Int) to (row[1] as String) }
                    }
                },
                chatTaskExecutor,
            )

            val unreadCountMap = unreadFuture.join()
            val opponentMap = opponentFuture.join()
            val postImageMap = postImageFuture.join()
            val auctionImageMap = auctionImageFuture.join()

            // 데이터 조합 및 클렌징 (유효하지 않은 데이터는 return@mapNotNull null로 건너뜀)
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
            }.sortedByDescending { it.lastMessageDate } // 최신 메시지가 위로 오도록 정렬

            log.debug("채팅 목록 조회 완료 - 사용자: {}, 조회된 방 개수: {}", me.nickname, responseList.size)
            return RsData("200-1", "채팅 목록 조회 성공", responseList)
        }

    /**
     * [채팅방 퇴장]
     * - 사용자가 방을 나가는 처리를 수행합니다.
     * - 구매자와 판매자 모두 방을 나갔을 때만 Soft Delete 처리합니다.
     */
    @Transactional
    fun exitChatRoom(roomId: String): RsData<Void?> {
        val room = findActiveRoom(roomId)
        val me = currentMemberFromDb()

        val myApiKey = requireApiKey(me)
        requireChatParticipant(room, myApiKey)
        room.exit(myApiKey) // 개별 유저 퇴장 플래그 업데이트

        if (room.isBothExited) {
            room.softDelete() // 양측 퇴장 시 채팅방 비활성화
        }

        return RsData("200-1", "채팅방에서 퇴장하였습니다.", null)
    }

    /**
     * [사용자 알림 전송]
     * 개인 알림 토픽(`/sub/user/{id}/notification`)으로 알림을 발행합니다.
     */
    private fun sendUserNotification(
        type: String,
        room: ChatRoom,
        recipient: Member,
        opponent: Member,
        message: String?,
        messageDate: LocalDateTime?,
    ) {
        // 메시징 전송 실패가 비즈니스 트랜잭션 전체를 롤백시키지 않도록 안전하게 감싸서 실행
        runMessagingSafely(
            onSuccess = { log.info("개인 알림 전송 - Type: {}, Recipient: {}, RoomID: {}", type, recipient.id, room.roomId) },
            onFailure = { e -> log.error("개인 알림 전송 실패 - Recipient: {}, Error: {}", recipient.id, e.message) },
        ) {
            val item = resolveNotificationItemInfo(room)

            // 현재 수신자의 미읽음 메시지 수 총합 계산
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

            messagingTemplate.convertAndSend("/sub/user/${recipient.id}/notification", notification)
        }
    }

    /**
     * 웹소켓 전송 시 발생할 수 있는 네트워크/세션 예외를 캡슐화하여 로깅하고,
     * 핵심 로직(DB 저장 등)에 영향을 주지 않도록 관리하는 헬퍼 함수입니다.
     */
    private inline fun runMessagingSafely(
        crossinline onSuccess: () -> Unit,
        crossinline onFailure: (Exception) -> Unit,
        block: () -> Unit,
    ) {
        runCatching(block)
            .onSuccess { onSuccess() }
            .onFailure { onFailure(it as? Exception ?: RuntimeException(it)) }
    }

    // --- 유틸리티 및 검증 메서드 ---

    /** 현재 인증된 회원을 DB에서 재조회 */
    private fun currentMemberFromDb(): Member {
        val actor = rq.actor ?: throw ServiceException("401-1", "로그인이 필요합니다.")
        return memberRepository.findById(actor.id)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 회원입니다.") }
    }

    private fun requireApiKey(member: Member): String =
        member.apiKey ?: throw ServiceException("500-1", "회원 apiKey가 없습니다.")

    /** 삭제되지 않은 활성 채팅방 조회 */
    private fun findActiveRoom(roomId: String): ChatRoom =
        chatRoomRepository.findByRoomIdAndDeletedFalse(roomId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 채팅방입니다.") }

    /** 해당 채팅방 참여자 권한 체크 */
    private fun requireChatParticipant(room: ChatRoom, apiKey: String) {
        if (room.sellerApiKey != apiKey && room.buyerApiKey != apiKey) {
            throw ServiceException("403-1", "해당 채팅방에 접근 권한이 없습니다.")
        }
    }

    /** 셀프 거래(본인 상품 채팅) 방지 */
    private fun ensureNotSelfTrade(seller: Member, buyerApiKey: String) {
        if (seller.apiKey == buyerApiKey) {
            throw ServiceException("400-3", "본인의 상품에는 채팅을 개설할 수 없습니다.")
        }
    }

    /** 거래 유형 문자열을 Enum으로 변환 */
    private fun parseTxType(txType: String): ChatRoomType =
        try {
            ChatRoomType.valueOf(txType.uppercase(Locale.ROOT))
        } catch (_: IllegalArgumentException) {
            throw ServiceException("400-2", "잘못된 거래 유형입니다.")
        }

    /** 목록 조회 시 사용할 아이템 요약 정보 매핑 (in-memory 캐싱 맵 활용) */
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

    /** 알림 전송 시점에 사용할 아이템 정보 매핑 (직접 조회 기반) */
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

    // --- 내부 데이터 홀더 ---
    private data class CreatedRoom(
        val room: ChatRoom,
        val seller: Member,
        val isNew: Boolean,
    )

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
