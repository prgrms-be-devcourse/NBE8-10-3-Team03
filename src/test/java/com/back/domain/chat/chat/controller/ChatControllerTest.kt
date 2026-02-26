package com.back.domain.chat.chat.controller

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.repository.CategoryRepository
import com.back.domain.chat.chat.dto.response.ChatResponse
import com.back.domain.chat.chat.entity.Chat
import com.back.domain.chat.chat.repository.ChatRepository
import com.back.domain.chat.chat.repository.ChatRoomRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.Role
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.post.post.entity.Post
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.repository.PostRepository
import com.back.global.security.SecurityUser
import com.back.global.security.StompHandler
import com.jayway.jsonpath.JsonPath
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChatControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var stompHandler: StompHandler

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var postRepository: PostRepository

    @Autowired
    private lateinit var auctionRepository: AuctionRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var chatRoomRepository: ChatRoomRepository

    @Autowired
    private lateinit var chatRepository: ChatRepository

    @MockitoBean
    private lateinit var messagingTemplate: SimpMessagingTemplate

    private lateinit var seller: Member
    private lateinit var buyer: Member
    private lateinit var anotherUser: Member

    private var salePostId: Int = 0
    private var auctionId: Int = 0
    private var soldPostId: Int = 0

    @BeforeEach
    fun setUp() {
        val category = categoryRepository.save(Category("디지털"))

        seller = Member("seller", "1234", "판매자").apply { role = Role.USER }
        buyer = Member("buyer", "1234", "구매자").apply { role = Role.USER }
        anotherUser = Member("another", "1234", "제3자").apply { role = Role.USER }

        memberRepository.save(seller)
        memberRepository.save(buyer)
        memberRepository.save(anotherUser)

        postRepository.save(
            Post(
                seller = seller,
                title = "판매 중 물품",
                content = "판매 중 입니다.",
                price = 10000,
                category = category,
                status = PostStatus.SALE,
            ),
        )

        postRepository.save(
            Post(
                seller = seller,
                title = "판매 완료된 물품",
                content = "이미 팔렸습니다.",
                price = 20000,
                category = category,
                status = PostStatus.SOLD,
            ),
        )

        auctionRepository.save(
            Auction.builder()
                .seller(seller)
                .category(category)
                .name("경매 물품")
                .description("경매 중 입니다.")
                .startPrice(1000)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(1))
                .build(),
        )

        em.flush()
        em.clear()

        seller = memberRepository.findByUsername("seller").orElseThrow()
        buyer = memberRepository.findByUsername("buyer").orElseThrow()
        anotherUser = memberRepository.findByUsername("another").orElseThrow()

        salePostId = postRepository.findAll().first { it.title == "판매 중 물품" }.id
        soldPostId = postRepository.findAll().first { it.title == "판매 완료된 물품" }.id
        auctionId = auctionRepository.findAll().first { it.name == "경매 물품" }.id
    }

    private fun makeSecurityUser(member: Member): SecurityUser = SecurityUser(
        member.id,
        member.username,
        "",
        member.nickname,
        member.role,
        member.authorities,
    )

    @Test
    @DisplayName("1. 일반 상품 채팅방 생성 성공")
    fun t1() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("itemId", salePostId.toString())
                .param("txType", "POST"),
        )
            .andDo(print())
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("2. 경매 상품 채팅방 생성 성공")
    fun t2() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("itemId", auctionId.toString())
                .param("txType", "AUCTION"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.roomId").isNotEmpty)
    }

    @Test
    @DisplayName("3. 이미 존재하는 방이면 기존 RoomId를 반환해야 한다")
    fun t3() {
        val firstRoomId = createRoomAsUser(salePostId, "POST", buyer)
        val secondRoomId = createRoomAsUser(salePostId, "POST", buyer)

        assertThat(firstRoomId).isEqualTo(secondRoomId)
    }

    @Test
    @DisplayName("4. 존재하지 않는 상품 ID 요청 시 예외 발생")
    fun t4() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("itemId", "999999")
                .param("txType", "POST"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-2"))
    }

    @Test
    @DisplayName("5. 존재하지 않는 회원(DB에 없는 ID) 요청 시 예외 발생")
    fun t5() {
        val unknownUser = SecurityUser(
            999999,
            "unknown",
            "",
            "unknown",
            Role.USER,
            listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(unknownUser))
                .param("itemId", salePostId.toString())
                .param("txType", "POST"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
            .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."))
    }

    @Test
    @DisplayName("6. 판매 중(SALE)이 아닌 상품(SOLD)에 채팅 시도 시 예외 발생")
    fun t6() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("itemId", soldPostId.toString())
                .param("txType", "POST"),
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("7. 본인(판매자)이 본인 상품에 채팅 시도 시 예외 발생")
    fun t7() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(seller)))
                .param("itemId", salePostId.toString())
                .param("txType", "POST"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-3"))
    }

    @Test
    @DisplayName("8. 지원하지 않는 거래 타입(TxType) 요청")
    fun t8() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("itemId", salePostId.toString())
                .param("txType", "UNKNOWN"),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("9. 본인(판매자)이 본인 경매에 채팅 시도 시 예외 발생")
    fun t9() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(seller)))
                .param("itemId", auctionId.toString())
                .param("txType", "AUCTION"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-3"))
            .andExpect(jsonPath("$.msg").value("본인의 상품에는 채팅을 개설할 수 없습니다."))
    }

    @Test
    @DisplayName("10. 메시지 전송 성공 (텍스트)")
    fun t10() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", roomId)
                .param("message", "구매 희망합니다."),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
    }

    @Test
    @DisplayName("11. 존재하지 않는 방 ID로 메시지 전송 시 실패")
    fun t11() {
        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", "invalid-room-uuid")
                .param("message", "Hello"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
            .andExpect(jsonPath("$.msg").value("존재하지 않는 채팅방입니다."))
    }

    @Test
    @DisplayName("12. 메시지 목록 조회 및 내용 검증")
    fun t12() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        sendMessageAsUser(roomId, "안녕하세요", buyer, null)
        sendMessageAsUser(roomId, "반갑습니다", seller, null)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].message").value("안녕하세요"))
            .andExpect(jsonPath("$.data[1].message").value("반갑습니다"))
    }

    @Test
    @DisplayName("13. 읽음 처리 확인 (상대방이 조회하면 isRead가 true)")
    fun t13() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        val chatId = sendMessageAsUser(roomId, "판매자 메시지", seller, null)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)

        val chat = chatRepository.findById(chatId).orElseThrow()
        assertThat(chat.read).isTrue()
    }

    @Test
    @DisplayName("14. lastChatId를 이용한 페이징(과거 내역 불러오기)")
    fun t14() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "1", buyer, null)
        sendMessageAsUser(roomId, "2", buyer, null)
        sendMessageAsUser(roomId, "3", buyer, null)

        val result = mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
            .andReturn()

        val data = objectMapper.readTree(result.response.contentAsString).path("data")
        val secondMsgId = data[1]["id"].asInt()

        mockMvc.perform(
            get("/api/v1/chat/room/$roomId")
                .with(user(makeSecurityUser(buyer)))
                .param("lastChatId", secondMsgId.toString()),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].message").value("1"))
    }

    @Test
    @DisplayName("15. 채팅 목록 조회 (본인과 관련된 방만 조회)")
    fun t15() {
        val myRoomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(myRoomId, "구매자 메시지", buyer, null)

        createRoomAsUser(salePostId, "POST", anotherUser).also { otherRoomId ->
            sendMessageAsUser(otherRoomId, "제3자 메시지", anotherUser, null)
        }

        em.flush()
        em.clear()

        val latestBuyer = memberRepository.findById(buyer.id).orElseThrow()
        val buyerChats = chatRepository.findAllLatestChatsByMember(latestBuyer.apiKey)

        assertThat(buyerChats).hasSize(1)
        assertThat(buyerChats[0].message).isEqualTo("구매자 메시지")
        assertThat(buyerChats[0].chatRoom.roomId).isEqualTo(myRoomId)
    }

    @Test
    @DisplayName("16. 채팅 목록 갱신 확인 (최신 메시지 반영)")
    fun t16() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "Old", buyer, null)
        sendMessageAsUser(roomId, "New", buyer, null)

        em.flush()
        em.clear()

        val latestBuyer = memberRepository.findById(buyer.id).orElseThrow()
        val buyerChats = chatRepository.findAllLatestChatsByMember(latestBuyer.apiKey)

        assertThat(buyerChats).hasSize(1)
        assertThat(buyerChats[0].message).isEqualTo("New")
        assertThat(buyerChats[0].chatRoom.roomId).isEqualTo(roomId)
    }

    @Test
    @DisplayName("17. 이미지와 함께 메시지 전송 성공")
    fun t17() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        val imageFile = MockMultipartFile("images", "test.jpg", "image/jpeg", "fake-image".toByteArray())

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .file(imageFile)
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", roomId)
                .param("message", "사진 보냅니다."),
        ).andExpect(status().isOk)
    }

    @Test
    @DisplayName("18. 이미지가 포함된 메시지 조회 시 ImageUrls 필드 검증")
    fun t18() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        val imageFile = MockMultipartFile("images", "test-image.png", "image/png", "test content".toByteArray())

        sendMessageAsUser(roomId, "포토 메시지", buyer, imageFile)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andDo(print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].message").value("포토 메시지"))
            .andExpect(jsonPath("$.data[0].imageUrls").isArray)
            .andExpect(jsonPath("$.data[0].imageUrls").isNotEmpty)
    }

    @Test
    @DisplayName("19. 인증되지 않은 사용자가 메시지 전송 시도 시 401 발생")
    @WithAnonymousUser
    fun t19() {
        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .with(csrf())
                .param("roomId", "any-room")
                .param("message", "비인증"),
        ).andExpect(status().isUnauthorized)
    }

    @Test
    @DisplayName("20. 참여자가 아닌 제3자가 메시지 목록 조회를 시도할 경우 403 차단")
    fun t20() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(anotherUser))))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @DisplayName("21. 메시지 전송 시 WebSocket 브로드캐스트 확인")
    fun t21() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", roomId)
                .param("message", "실시간 확인"),
        ).andExpect(status().isOk)

        verify(messagingTemplate, times(1))
            .convertAndSend(eq("/sub/v1/chat/room/$roomId"), any(ChatResponse::class.java))
    }

    @Test
    @DisplayName("22. 빈 이미지 파일이 포함된 경우 무시하고 메시지만 저장")
    fun t22() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        val emptyFile = MockMultipartFile("images", "", "image/jpeg", ByteArray(0))

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .file(emptyFile)
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", roomId)
                .param("message", "빈 파일 테스트"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.chatId").exists())
    }

    @Test
    @DisplayName("23. 대량의 읽지 않은 메시지가 한 번의 조회로 모두 true로 변경되는지 확인")
    fun t23() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        repeat(5) { idx -> sendMessageAsUser(roomId, "msg $idx", seller, null) }

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[?(@.isRead == false)]").doesNotExist())
    }

    @Test
    @DisplayName("24. 채팅방 나가기 요청 성공")
    fun t24() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        val latestBuyer = memberRepository.findById(buyer.id).orElseThrow()

        mockMvc.perform(
            patch("/api/v1/chat/room/$roomId/exit")
                .with(csrf())
                .with(user(makeSecurityUser(latestBuyer))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
    }

    @Test
    @DisplayName("25. 채팅방 나가기 후 목록 조회 시 필터링 확인")
    fun t25() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "안녕하세요", seller, null)

        val latestBuyer = memberRepository.findById(buyer.id).orElseThrow()
        val latestSeller = memberRepository.findById(seller.id).orElseThrow()

        mockMvc.perform(patch("/api/v1/chat/room/$roomId/exit").with(csrf()).with(user(makeSecurityUser(latestBuyer))))
            .andExpect(status().isOk)

        em.flush()
        em.clear()

        val buyerChats = chatRepository.findAllLatestChatsByMember(memberRepository.findById(buyer.id).orElseThrow().apiKey)
        val sellerChats = chatRepository.findAllLatestChatsByMember(memberRepository.findById(seller.id).orElseThrow().apiKey)

        assertThat(buyerChats).isEmpty()
        assertThat(sellerChats).hasSize(1)
        assertThat(latestSeller.id).isEqualTo(seller.id)
    }

    @Test
    @DisplayName("26. 참여자가 아닌 제3자가 채팅방 나가기를 시도하면 차단(403) 되어야 한다")
    fun t26() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(
            patch("/api/v1/chat/room/$roomId/exit")
                .with(csrf())
                .with(user(makeSecurityUser(anotherUser))),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @DisplayName("27. 채팅방 참여자(구매자)가 해당 방 구독(SUBSCRIBE) 시 성공해야 한다")
    fun t27() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/sub/chat/room/$roomId"
        accessor.user = UsernamePasswordAuthenticationToken(makeSecurityUser(buyer), null)

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertDoesNotThrow { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("28. 참여자가 아닌 제3자가 채팅방 구독(SUBSCRIBE) 시 예외 발생(구독 차단)")
    fun t28() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/sub/v1/chat/room/$roomId"
        accessor.user = UsernamePasswordAuthenticationToken(makeSecurityUser(anotherUser), null)

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertThrows(RuntimeException::class.java) { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("29. 채팅방이 아닌 공개 채널(예: 경매) 구독 시에는 권한 체크 없이 통과해야 한다")
    fun t29() {
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/sub/v1/auction/$auctionId"
        accessor.user = UsernamePasswordAuthenticationToken(makeSecurityUser(anotherUser), null)

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertDoesNotThrow { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("30. 다중 이미지(2장) 업로드 및 조회 검증")
    fun t30() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        val image1 = MockMultipartFile("images", "img1.jpg", "image/jpeg", "image_content_1".toByteArray())
        val image2 = MockMultipartFile("images", "img2.jpg", "image/jpeg", "image_content_2".toByteArray())

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .file(image1)
                .file(image2)
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", roomId)
                .param("message", "사진 2장 보냅니다."),
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].imageUrls.length()").value(2))
    }

    @Test
    @DisplayName("31. [Stomp] CONNECT 시 잘못된 토큰이면 연결 거부(예외 발생)")
    fun t31() {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        accessor.setNativeHeader("Authorization", "INVALID_TOKEN_FORMAT")

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertThrows(RuntimeException::class.java) { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("32. 내가 보낸 메시지는 상대방이 읽기 전까지 isRead가 false여야 한다")
    fun t32() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "안 읽음 테스트", buyer, null)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].senderId").value(buyer.id))
            .andExpect(jsonPath("$.data[0].read").value(false))
    }

    @Test
    @DisplayName("33. 이미 나간 방에 다시 나가기 요청 시에도 성공(Idempotent) 혹은 적절한 처리")
    fun t33() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(patch("/api/v1/chat/room/$roomId/exit").with(csrf()).with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)

        mockMvc.perform(patch("/api/v1/chat/room/$roomId/exit").with(csrf()).with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("34. 인증되지 않은 사용자가 채팅방 생성을 시도하면 401을 반환한다")
    fun t34() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .param("itemId", salePostId.toString())
                .param("txType", "POST"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.resultCode").value("401-1"))
    }

    @Test
    @DisplayName("35. 채팅방 비참여자가 메시지 전송 시도 시 403을 반환한다")
    fun t35() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .with(csrf())
                .with(user(makeSecurityUser(anotherUser)))
                .param("roomId", roomId)
                .param("message", "권한 없는 사용자 전송"),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @DisplayName("36. 내 채팅 목록 API 조회 성공 (채팅 내역이 없으면 빈 배열)")
    fun t36() {
        mockMvc.perform(get("/api/v1/chat/list").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.resultCode").value("200-1"))
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    @Test
    @DisplayName("37. 존재하지 않는 채팅방 메시지 조회 시 404를 반환한다")
    fun t37() {
        mockMvc.perform(get("/api/v1/chat/room/not-exist-room-id").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("38. 존재하지 않는 채팅방 나가기 시도 시 404를 반환한다")
    fun t38() {
        mockMvc.perform(
            patch("/api/v1/chat/room/not-exist-room-id/exit")
                .with(csrf())
                .with(user(makeSecurityUser(buyer))),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("39. 메시지와 이미지가 모두 비어 있으면 유효성 검사 실패(400)")
    fun t39() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", roomId)
                .param("message", "   "),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.resultCode").value("400-1"))
            .andExpect(jsonPath("$.msg", containsString("메시지 내용이 없으면 최소 한 장의 이미지가 필요합니다.")))
    }

    @Test
    @DisplayName("40. 양측 모두 나가면 채팅방은 soft delete 되어 조회되지 않아야 한다")
    fun t40() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "초기 메시지", buyer, null)

        mockMvc.perform(patch("/api/v1/chat/room/$roomId/exit").with(csrf()).with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)

        mockMvc.perform(patch("/api/v1/chat/room/$roomId/exit").with(csrf()).with(user(makeSecurityUser(seller))))
            .andExpect(status().isOk)

        em.flush()
        em.clear()

        val latestBuyer = memberRepository.findById(buyer.id).orElseThrow()
        val latestSeller = memberRepository.findById(seller.id).orElseThrow()

        assertThat(chatRoomRepository.findByRoomIdAndDeletedFalse(roomId)).isNull()
        assertThat(chatRepository.findAllLatestChatsByMember(latestBuyer.apiKey)).isEmpty()
        assertThat(chatRepository.findAllLatestChatsByMember(latestSeller.apiKey)).isEmpty()
    }

    @Test
    @DisplayName("41. 신규 채팅방 생성 시 판매자 개인 알림 토픽으로 전송된다")
    fun t41() {
        createRoomAsUser(salePostId, "POST", buyer)

        verify(messagingTemplate, atLeastOnce())
            .convertAndSend(eq("/sub/user/${seller.id}/notification"), any(Any::class.java))
    }

    @Test
    @DisplayName("42. 메시지 조회로 읽음 처리되면 read 알림 토픽으로 전송된다")
    fun t42() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "읽음 알림 테스트", seller, null)
        reset(messagingTemplate)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)

        verify(messagingTemplate, atLeastOnce())
            .convertAndSend(eq("/sub/v1/chat/room/$roomId/read"), any(Any::class.java))
    }

    @Test
    @DisplayName("43. [Stomp] 본인 개인 알림 토픽 구독은 허용되어야 한다")
    fun t43() {
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/sub/user/${buyer.id}/notification"
        accessor.user = UsernamePasswordAuthenticationToken(makeSecurityUser(buyer), null)

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertDoesNotThrow { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("44. [Stomp] 타인 개인 알림 토픽 구독은 차단되어야 한다")
    fun t44() {
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/sub/user/${seller.id}/notification"
        accessor.user = UsernamePasswordAuthenticationToken(makeSecurityUser(buyer), null)

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertThrows(RuntimeException::class.java) { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("45. [Stomp] 채팅방 참여자는 read 토픽 구독이 허용되어야 한다")
    fun t45() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/sub/v1/chat/room/$roomId/read"
        accessor.user = UsernamePasswordAuthenticationToken(makeSecurityUser(buyer), null)

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertDoesNotThrow { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("46. [Stomp] 채팅방 비참여자는 read 토픽 구독이 차단되어야 한다")
    fun t46() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.destination = "/sub/v1/chat/room/$roomId/read"
        accessor.user = UsernamePasswordAuthenticationToken(makeSecurityUser(anotherUser), null)

        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val mockChannel = mock(MessageChannel::class.java)

        assertThrows(RuntimeException::class.java) { stompHandler.preSend(message, mockChannel) }
    }

    @Test
    @DisplayName("47. 메시지 전송 시 상대방 개인 알림 토픽으로 NEW_MESSAGE 알림이 전송된다")
    fun t47() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        reset(messagingTemplate)

        sendMessageAsUser(roomId, "개인 알림 테스트", buyer, null)

        verify(messagingTemplate, atLeastOnce())
            .convertAndSend(eq("/sub/user/${seller.id}/notification"), any(Any::class.java))
    }

    @Test
    @DisplayName("48. 조회 시 상대방 메시지가 없으면 read 알림 토픽은 전송되지 않는다")
    fun t48() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "내가 보낸 메시지", buyer, null)
        reset(messagingTemplate)

        mockMvc.perform(get("/api/v1/chat/room/$roomId").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)

        verify(messagingTemplate, never())
            .convertAndSend(eq("/sub/v1/chat/room/$roomId/read"), any(Any::class.java))
    }

    @Test
    @DisplayName("49. 존재하지 않는 경매 ID로 채팅방 생성 요청 시 404-3을 반환한다")
    fun t49() {
        mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("itemId", "999999")
                .param("txType", "AUCTION"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-3"))
    }

    @Test
    @DisplayName("50. 채팅 목록 API는 txType/item/unreadCount 필드를 포함해 반환한다")
    fun t50() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)
        sendMessageAsUser(roomId, "미읽음 메시지", seller, null)

        mockMvc.perform(get("/api/v1/chat/list").with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].roomId").value(roomId))
            .andExpect(jsonPath("$.data[0].txType").value("POST"))
            .andExpect(jsonPath("$.data[0].itemId").value(salePostId))
            .andExpect(jsonPath("$.data[0].unreadCount").value(1))
    }

    @Test
    @DisplayName("51. 양측 퇴장으로 soft delete 된 방은 메시지 전송 시 404를 반환한다")
    fun t51() {
        val roomId = createRoomAsUser(salePostId, "POST", buyer)

        mockMvc.perform(patch("/api/v1/chat/room/$roomId/exit").with(csrf()).with(user(makeSecurityUser(buyer))))
            .andExpect(status().isOk)
        mockMvc.perform(patch("/api/v1/chat/room/$roomId/exit").with(csrf()).with(user(makeSecurityUser(seller))))
            .andExpect(status().isOk)

        mockMvc.perform(
            multipart("/api/v1/chat/send")
                .with(csrf())
                .with(user(makeSecurityUser(buyer)))
                .param("roomId", roomId)
                .param("message", "재접근 시도"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.resultCode").value("404-1"))
    }

    private fun createRoomAsUser(itemId: Int, txType: String, member: Member): String {
        val result = mockMvc.perform(
            post("/api/v1/chat/room")
                .with(csrf())
                .with(user(makeSecurityUser(member)))
                .param("itemId", itemId.toString())
                .param("txType", txType),
        ).andReturn()

        return JsonPath.read(result.response.contentAsString, "$.data.roomId")
    }

    private fun sendMessageAsUser(roomId: String, message: String, member: Member, imageFile: MockMultipartFile?): Int {
        var builder = multipart("/api/v1/chat/send")
            .with(csrf())
            .with(user(makeSecurityUser(member)))
            .param("roomId", roomId)
            .param("message", message)

        imageFile?.let { builder = builder.file(it) }

        val responseBody = mockMvc.perform(builder)
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return JsonPath.read(responseBody, "$.data.chatId")
    }
}
