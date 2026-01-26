package com.back.domain.chat.chat.controller;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.chat.chat.dto.response.ChatResponse;
import com.back.domain.chat.chat.entity.Chat;
import com.back.domain.chat.chat.repository.ChatRepository;
import com.back.domain.chat.chat.repository.ChatRoomRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostStatus;
import com.back.domain.post.post.repository.PostRepository;
import com.back.global.security.SecurityUser;
import com.back.global.security.StompHandler;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager em;
    @Autowired
    private StompHandler stompHandler;

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private ChatRepository chatRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private Member seller;
    private Member buyer;
    private Member anotherUser;

    private int salePostId;
    private int auctionId;
    private int soldPostId;

    // 테스트 데이터 셋팅
    @BeforeEach
    void setUp() {
        Category category = new Category("디지털");
        categoryRepository.save(category);

        seller = new Member("seller", "1234", "판매자");
        seller.setRole(Role.USER);
        memberRepository.save(seller);

        buyer = new Member("buyer", "1234", "구매자");
        buyer.setRole(Role.USER);
        memberRepository.save(buyer);

        anotherUser = new Member("another", "1234", "제3자");
        anotherUser.setRole(Role.USER);
        memberRepository.save(anotherUser);

        Post salePost = Post.builder()
                .title("판매 중 물품")
                .content("판매 중 입니다.")
                .price(10000)
                .seller(seller)
                .status(PostStatus.SALE)
                .build();
        postRepository.save(salePost);

        Post soldPost = Post.builder()
                .title("판매 완료된 물품")
                .content("이미 팔렸습니다.")
                .price(20000)
                .seller(seller)
                .status(PostStatus.SOLD)
                .build();
        postRepository.save(soldPost);

        Auction openAuction = Auction.builder()
                .seller(seller)
                .category(category)
                .name("경매 물품")
                .description("경매 중 입니다.")
                .startPrice(1000)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(1))
                .build();
        auctionRepository.save(openAuction);

        em.flush();
        em.clear();

        // 영속성 컨텍스트 초기화 후 다시 불러오기 (ID값 유지)
        seller = memberRepository.findByUsername("seller").orElseThrow();
        buyer = memberRepository.findByUsername("buyer").orElseThrow();
        anotherUser = memberRepository.findByUsername("another").orElseThrow();

        // 저장된 Post, Auction ID 가져오기
        Post savedSalePost = postRepository.findAll().stream()
                .filter(p -> p.getTitle().equals("판매 중 물품"))
                .findFirst().orElseThrow();
        this.salePostId = savedSalePost.getId();

        Post savedSoldPost = postRepository.findAll().stream()
                .filter(p -> p.getTitle().equals("판매 완료된 물품"))
                .findFirst().orElseThrow();
        this.soldPostId = savedSoldPost.getId();

        Auction savedAuction = auctionRepository.findAll().stream()
                .filter(a -> a.getName().equals("경매 물품"))
                .findFirst().orElseThrow();
        this.auctionId = savedAuction.getId();
    }

    // SecurityUser 생성
    private SecurityUser makeSecurityUser(Member member) {
        return new SecurityUser(
                member.getId(),
                member.getUsername(),
                "",
                member.getNickname(),
                member.getRole(),
                member.getAuthorities()
        );
    }

    @Test
    @DisplayName("1. 일반 상품 채팅방 생성 성공")
    void t1() throws Exception {
        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "POST"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("2. 경매 상품 채팅방 생성 성공")
    void t2() throws Exception {
        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("itemId", String.valueOf(auctionId))
                        .param("txType", "AUCTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.roomId").isNotEmpty());
    }

    @Test
    @DisplayName("3. 이미 존재하는 방이면 기존 RoomId를 반환해야 한다")
    void t3() throws Exception {
        String firstRoomId = createRoomAsUser(salePostId, "POST", buyer);
        String secondRoomId = createRoomAsUser(salePostId, "POST", buyer);

        assertThat(firstRoomId).isEqualTo(secondRoomId);
    }

    @Test
    @DisplayName("4. 존재하지 않는 상품 ID 요청 시 예외 발생")
    void t4() throws Exception {
        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("itemId", "999999")
                        .param("txType", "POST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"));
    }

    @Test
    @DisplayName("5. 존재하지 않는 회원(DB에 없는 ID) 요청 시 예외 발생")
    void t5() throws Exception {
        SecurityUser unknownUser = new SecurityUser(
                999999, "unknown", "", "unknown", Role.USER, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(unknownUser))
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "POST"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @DisplayName("6. 판매 중(SALE)이 아닌 상품(SOLD)에 채팅 시도 시 예외 발생")
    void t6() throws Exception {
        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("itemId", String.valueOf(soldPostId))
                        .param("txType", "POST"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));
    }

    @Test
    @DisplayName("7. 본인(판매자)이 본인 상품에 채팅 시도 시 예외 발생")
    void t7() throws Exception {
        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(seller)))
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "POST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));
    }

    @Test
    @DisplayName("8. 지원하지 않는 거래 타입(TxType) 요청")
    void t8() throws Exception {
        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. 본인(판매자)이 본인 경매에 채팅 시도 시 예외 발생")
    void t9() throws Exception {
        mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(seller)))
                        .param("itemId", String.valueOf(auctionId))
                        .param("txType", "AUCTION"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"))
                .andExpect(jsonPath("$.msg").value("본인의 상품에는 채팅을 개설할 수 없습니다."));
    }

    @Test
    @DisplayName("10. 메시지 전송 성공 (텍스트)")
    void t10() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        mockMvc.perform(multipart("/api/v1/chat/send")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("roomId", roomId)
                        .param("message", "구매 희망합니다."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("11. 존재하지 않는 방 ID로 메시지 전송 시 실패")
    void t11() throws Exception {
        mockMvc.perform(multipart("/api/v1/chat/send")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("roomId", "invalid-room-uuid")
                        .param("message", "Hello"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 채팅방입니다."));
    }

    @Test
    @DisplayName("12. 메시지 목록 조회 및 내용 검증")
    void t12() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        sendMessageAsUser(roomId, "안녕하세요", buyer, null);
        sendMessageAsUser(roomId, "반갑습니다", seller, null);

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].message").value("안녕하세요"))
                .andExpect(jsonPath("$.data[1].message").value("반갑습니다"));
    }

    @Test
    @DisplayName("13. 읽음 처리 확인 (상대방이 조회하면 isRead가 true)")
    void t13() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);
        int chatId = sendMessageAsUser(roomId, "판매자 메시지", seller, null);

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk());

        Chat chat = chatRepository.findById(chatId).orElseThrow();

        org.assertj.core.api.Assertions.assertThat(chat.getRead()).isTrue();
    }

    @Test
    @DisplayName("14. lastChatId를 이용한 페이징(이어보기)")
    void t14() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);
        sendMessageAsUser(roomId, "1", buyer, null);
        sendMessageAsUser(roomId, "2", buyer, null);
        sendMessageAsUser(roomId, "3", buyer, null);

        MvcResult result = mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer))))
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        int secondMsgId = data.get(1).get("id").asInt();

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer)))
                        .param("lastChatId", String.valueOf(secondMsgId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].message").value("3"));
    }

    @Test
    @DisplayName("15. 채팅 목록 조회 (본인과 관련된 방만 조회)")
    void t15() throws Exception {
        String myRoomId = createRoomAsUser(salePostId, "POST", buyer);
        sendMessageAsUser(myRoomId, "구매자 메시지", buyer, null);

        String otherRoomId = createRoomAsUser(salePostId, "POST", anotherUser);
        sendMessageAsUser(otherRoomId, "제3자 메시지", anotherUser, null);

        mockMvc.perform(get("/api/v1/chat/list")
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].lastMessage").value("구매자 메시지"))
                .andExpect(jsonPath("$.data[0].opponentNickname").exists())
                .andExpect(jsonPath("$.data[0].roomId").value(myRoomId));
    }

    @Test
    @DisplayName("16. 채팅 목록 갱신 확인 (최신 메시지 반영)")
    void t16() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        sendMessageAsUser(roomId, "Old", buyer, null);
        sendMessageAsUser(roomId, "New", buyer, null);

        mockMvc.perform(get("/api/v1/chat/list")
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lastMessage").value("New"))
                .andExpect(jsonPath("$.data[0].unreadCount").exists());
    }

    @Test
    @DisplayName("17. 이미지와 함께 메시지 전송 성공")
    void t17() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/chat/send")
                        .file(imageFile)
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("roomId", roomId)
                        .param("message", "사진 보냅니다."))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("18. 이미지가 포함된 메시지 조회 시 ImageUrls 필드 검증")
    void t18() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        MockMultipartFile imageFile = new MockMultipartFile(
                "images",
                "test-image.png",
                "image/png",
                "test content".getBytes()
        );

        sendMessageAsUser(roomId, "포토 메시지", buyer, imageFile);

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer))))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].message").value("포토 메시지"))
                .andExpect(jsonPath("$.data[0].imageUrls").isArray())
                .andExpect(jsonPath("$.data[0].imageUrls").isNotEmpty());
    }

    @Test
    @DisplayName("19. 인증되지 않은 사용자가 메시지 전송 시도 시 401 발생")
    @WithAnonymousUser
    void t19() throws Exception {
        mockMvc.perform(multipart("/api/v1/chat/send")
                        .with(csrf())
                        .param("roomId", "any-room")
                        .param("message", "비인증"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("20. 참여자가 아닌 제3자가 메시지 목록 조회를 시도할 경우 403 차단")
    void t20() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(anotherUser))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("21. 메시지 전송 시 WebSocket 브로드캐스트 확인")
    void t21() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        mockMvc.perform(multipart("/api/v1/chat/send")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("roomId", roomId)
                        .param("message", "실시간 확인"))
                .andExpect(status().isOk());

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/sub/v1/chat/room/" + roomId), any(ChatResponse.class));
    }

    @Test
    @DisplayName("22. 빈 이미지 파일이 포함된 경우 무시하고 메시지만 저장")
    void t22() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);
        MockMultipartFile emptyFile = new MockMultipartFile("images", "", "image/jpeg", new byte[0]);

        mockMvc.perform(multipart("/api/v1/chat/send")
                        .file(emptyFile)
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("roomId", roomId)
                        .param("message", "빈 파일 테스트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatId").exists());
    }

    @Test
    @DisplayName("23. 대량의 읽지 않은 메시지가 한 번의 조회로 모두 true로 변경되는지 확인")
    void t23() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);
        for(int i=0; i<5; i++) {
            sendMessageAsUser(roomId, "msg " + i, seller, null);
        }

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.isRead == false)]").doesNotExist());
    }

    @Test
    @DisplayName("24. 채팅방 나가기 요청 성공")
    void t24() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        Member latestBuyer = memberRepository.findById(buyer.getId()).get();

        mockMvc.perform(patch("/api/v1/chat/room/" + roomId + "/exit")
                        .with(csrf())
                        .with(user(makeSecurityUser(latestBuyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("25. 채팅방 나가기 후 목록 조회 시 필터링 확인")
    void t25() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);
        sendMessageAsUser(roomId, "안녕하세요", seller, null);

        Member latestBuyer = memberRepository.findById(buyer.getId()).orElse(buyer);
        Member latestSeller = memberRepository.findById(seller.getId()).orElse(seller);

        mockMvc.perform(patch("/api/v1/chat/room/" + roomId + "/exit")
                        .with(csrf())
                        .with(user(makeSecurityUser(latestBuyer))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/chat/list")
                        .with(user(makeSecurityUser(latestBuyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/chat/list")
                        .with(user(makeSecurityUser(latestSeller))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("26. 참여자가 아닌 제3자가 채팅방 나가기를 시도하면 차단(403) 되어야 한다")
    void t26() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        mockMvc.perform(patch("/api/v1/chat/room/" + roomId + "/exit")
                        .with(csrf())
                        .with(user(makeSecurityUser(anotherUser))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @DisplayName("27. 채팅방 참여자(구매자)가 해당 방 구독(SUBSCRIBE) 시 성공해야 한다")
    void t27() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/chat/room/" + roomId);

        Authentication auth = new UsernamePasswordAuthenticationToken(makeSecurityUser(buyer), null);
        accessor.setUser(auth);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel mockChannel = mock(MessageChannel.class);

        assertDoesNotThrow(() -> stompHandler.preSend(message, mockChannel));
    }

    @Test
    @DisplayName("28. 참여자가 아닌 제3자가 채팅방 구독(SUBSCRIBE) 시 예외 발생(구독 차단)")
    void t28() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/sub/v1/chat/room/" + roomId);

        Authentication auth = new UsernamePasswordAuthenticationToken(makeSecurityUser(anotherUser), null);
        accessor.setUser(auth);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel mockChannel = mock(MessageChannel.class);

        assertThrows(RuntimeException.class, () -> stompHandler.preSend(message, mockChannel));
    }

    @Test
    @DisplayName("29. 채팅방이 아닌 공개 채널(예: 경매) 구독 시에는 권한 체크 없이 통과해야 한다")
    void t29() throws Exception {
        String publicChannel = "/sub/v1/auction/" + auctionId;

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(publicChannel);

        Authentication auth = new UsernamePasswordAuthenticationToken(makeSecurityUser(anotherUser), null);
        accessor.setUser(auth);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel mockChannel = mock(MessageChannel.class);

        assertDoesNotThrow(() -> stompHandler.preSend(message, mockChannel));
    }

    @Test
    @DisplayName("30. 다중 이미지(2장) 업로드 및 조회 검증")
    void t30() throws Exception {
        // Given
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        MockMultipartFile image1 = new MockMultipartFile(
                "images", "img1.jpg", "image/jpeg", "image_content_1".getBytes()
        );
        MockMultipartFile image2 = new MockMultipartFile(
                "images", "img2.jpg", "image/jpeg", "image_content_2".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/chat/send")
                        .file(image1)
                        .file(image2)
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer)))
                        .param("roomId", roomId)
                        .param("message", "사진 2장 보냅니다."))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].imageUrls.length()").value(2));
    }

    @Test
    @DisplayName("31. [Stomp] CONNECT 시 잘못된 토큰이면 연결 거부(예외 발생)")
    void t31() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);

        accessor.setNativeHeader("Authorization", "INVALID_TOKEN_FORMAT");

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        MessageChannel mockChannel = mock(MessageChannel.class);

        assertThrows(RuntimeException.class, () -> {
            stompHandler.preSend(message, mockChannel);
        }, "Unauthorized: Invalid token format");
    }

    @Test
    @DisplayName("32. 내가 보낸 메시지는 상대방이 읽기 전까지 isRead가 false여야 한다")
    void t32() throws Exception {
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        sendMessageAsUser(roomId, "안 읽음 테스트", buyer, null);

        mockMvc.perform(get("/api/v1/chat/room/" + roomId)
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].senderId").value(buyer.getId()))
                .andExpect(jsonPath("$.data[0].read").value(false));
    }

    @Test
    @DisplayName("33. 이미 나간 방에 다시 나가기 요청 시에도 성공(Idempotent) 혹은 적절한 처리")
    void t33() throws Exception {
        // Given
        String roomId = createRoomAsUser(salePostId, "POST", buyer);

        mockMvc.perform(patch("/api/v1/chat/room/" + roomId + "/exit")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/chat/room/" + roomId + "/exit")
                        .with(csrf())
                        .with(user(makeSecurityUser(buyer))))
                .andExpect(status().isOk());
    }

    // --- [헬퍼 메서드] ---
    // 채팅방 생성 후 Room ID(UUID String) 반환
    private String createRoomAsUser(int itemId, String txType, Member member) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/chat/room")
                        .with(csrf())
                        .with(user(makeSecurityUser(member)))
                        .param("itemId", String.valueOf(itemId))
                        .param("txType", txType))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("roomId").asText();
    }

    // 메시지 전송 (텍스트 + 선택적 이미지)
    private int sendMessageAsUser(String roomId, String message, Member member, MockMultipartFile imageFile) throws Exception {
        var builder = multipart("/api/v1/chat/send")
                .with(csrf())
                .with(user(makeSecurityUser(member)))
                .param("roomId", roomId)
                .param("message", message);

        if (imageFile != null) builder.file(imageFile);

        String responseBody = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(responseBody, "$.data.chatId");
    }
}