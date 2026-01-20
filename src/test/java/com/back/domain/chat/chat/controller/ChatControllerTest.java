package com.back.domain.chat.chat.controller;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostStatus;
import com.back.domain.post.post.repository.PostRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@WithMockUser
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EntityManager em;

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    private Member seller;
    private Member buyer;
    private Member anotherUser;

    private Post salePost;
    private Post soldPost;
    private Auction openAuction;

    private int salePostId;
    private int soldPostId;
    private int auctionId;

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

        salePost = Post.builder()
                .title("판매 중 물품")
                .content("판매 중 입니다.")
                .price(10000)
                .seller(seller)
                .status(PostStatus.SALE)
                .build();
        postRepository.save(salePost);
        salePostId = salePost.getId();

        soldPost = Post.builder()
                .title("판매완료 물품")
                .content("판매완료 입니다.")
                .price(5000)
                .seller(seller)
                .status(PostStatus.SOLD)
                .build();
        postRepository.save(soldPost);
        soldPostId = soldPost.getId();

        openAuction = Auction.builder()
                .seller(seller)
                .category(category)
                .name("경매 물품")
                .description("경매 중 입니다.")
                .startPrice(1000)
                .startAt(LocalDateTime.now())
                .endAt(LocalDateTime.now().plusDays(1))
                .build();
        auctionRepository.save(openAuction);
        auctionId = openAuction.getId();
    }

    @Test
    @DisplayName("1. 일반 상품 채팅방 생성 성공")
    void t1() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "POST")
                        .param("buyerApiKey", buyer.getApiKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.roomId").isNotEmpty());
    }

    @Test
    @DisplayName("2. 경매 상품 채팅방 생성 성공")
    void t2() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(auctionId))
                        .param("txType", "AUCTION")
                        .param("buyerApiKey", buyer.getApiKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.roomId").isNotEmpty());
    }

    @Test
    @DisplayName("3. 이미 존재하는 방이면 기존 RoomId를 반환해야 한다")
    void t3() throws Exception {
        String firstRoomId = createRoom(salePostId, "POST", buyer.getApiKey());
        String secondRoomId = createRoom(salePostId, "POST", buyer.getApiKey());

        assertThat(firstRoomId).isEqualTo(secondRoomId);
    }

    @Test
    @DisplayName("4. 존재하지 않는 상품 ID 요청 시 예외 발생")
    void t4() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", "999999")
                        .param("txType", "POST")
                        .param("buyerApiKey", buyer.getApiKey()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-2"))
                .andExpect(jsonPath("$.msg").value("해당 게시글이 존재하지 않습니다."));
    }

    @Test
    @DisplayName("5. 존재하지 않는 회원(API Key) 요청 시 예외 발생")
    void t5() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "POST")
                        .param("buyerApiKey", "invalid-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 회원입니다."));
    }

    @Test
    @DisplayName("6. 판매 중(SALE)이 아닌 상품(SOLD)에 채팅 시도 시 예외 발생")
    void t6() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(soldPostId))
                        .param("txType", "POST")
                        .param("buyerApiKey", buyer.getApiKey()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("판매 중인 상품이 아니므로 채팅을 시작할 수 없습니다."));
    }

    @Test
    @DisplayName("7. 본인(판매자)이 본인 상품에 채팅 시도 시 예외 발생")
    void t7() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "POST")
                        .param("buyerApiKey", seller.getApiKey()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"))
                .andExpect(jsonPath("$.msg").value("본인의 상품에는 채팅을 개설할 수 없습니다."));
    }

    @Test
    @DisplayName("8. 지원하지 않는 거래 타입(TxType) 요청")
    void t8() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(salePostId))
                        .param("txType", "UNKNOWN")
                        .param("buyerApiKey", buyer.getApiKey()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("9. 본인(판매자)이 본인 경매에 채팅 시도 시 예외 발생")
    void t9() throws Exception {
        mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(auctionId))
                        .param("txType", "AUCTION")
                        .param("buyerApiKey", seller.getApiKey()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"))
                .andExpect(jsonPath("$.msg").value("본인의 상품에는 채팅을 개설할 수 없습니다."));
    }

    @Test
    @DisplayName("10. 메시지 전송 성공 (텍스트)")
    void t10() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());

        mockMvc.perform(multipart("/api/chat/send")
                        .with(csrf())
                        .param("roomId", roomId)
                        .param("sender", buyer.getNickname())
                        .param("message", "구매 희망합니다."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.chatId").exists());
    }

    @Test
    @DisplayName("11. 존재하지 않는 방 ID로 메시지 전송 시 실패")
    void t11() throws Exception {
        mockMvc.perform(multipart("/api/chat/send")
                        .with(csrf())
                        .param("roomId", "invalid-room-uuid")
                        .param("sender", buyer.getNickname())
                        .param("message", "Hello"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 채팅방입니다."));
    }

    @Test
    @DisplayName("12. 메시지 목록 조회 및 내용 검증")
    void t12() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, buyer.getNickname(), "안녕하세요", null);
        sendMessage(roomId, seller.getNickname(), "반갑습니다", null);

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].message").value("안녕하세요"))
                .andExpect(jsonPath("$.data[1].message").value("반갑습니다"));
    }

    @Test
    @DisplayName("13. 읽음 처리 확인 (상대방이 조회하면 isRead가 true)")
    void t13() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, seller.getNickname(), "메시지", null);

        mockMvc.perform(get("/api/chat/room/" + roomId)
                        .param("readerName", buyer.getNickname()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isRead").value(true));
    }
    @Test
    @DisplayName("14. lastChatId를 이용한 페이징(이어보기)")
    void t14() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, buyer.getNickname(), "1", null);
        sendMessage(roomId, buyer.getNickname(), "2", null);
        sendMessage(roomId, buyer.getNickname(), "3", null);

        MvcResult result = mockMvc.perform(get("/api/chat/room/" + roomId)).andReturn();
        String jsonStr = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(jsonStr);
        JsonNode data = root.path("data");
        int secondMsgId = data.get(1).get("id").asInt();

        mockMvc.perform(get("/api/chat/room/" + roomId)
                        .param("lastChatId", String.valueOf(secondMsgId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].message").value("3"));
    }

    @Test
    @DisplayName("15. 채팅 목록 조회 (여러 방이 있을 때)")
    void t15() throws Exception {
        String room1 = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(room1, buyer.getNickname(), "Room1 Msg", null);

        String room2 = createRoom(salePostId, "POST", anotherUser.getApiKey());
        sendMessage(room2, anotherUser.getNickname(), "Room2 Msg", null);

        mockMvc.perform(get("/api/chat/list"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    assertThat(json).contains("Room1 Msg");
                    assertThat(json).contains("Room2 Msg");
                });
    }

    @Test
    @DisplayName("16. 채팅 목록 갱신 확인 (최신 메시지 반영)")
    void t16() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, buyer.getNickname(), "Old", null);
        sendMessage(roomId, buyer.getNickname(), "New", null);

        mockMvc.perform(get("/api/chat/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].message").value("New"));
    }

    @Test
    @DisplayName("17. 이미지와 함께 메시지 전송 성공")
    void t17() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());

        // 가짜 이미지 파일 생성
        MockMultipartFile imageFile = new MockMultipartFile(
                "images", "test.jpg", "image/jpeg", "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/api/chat/send")
                        .file(imageFile)
                        .with(csrf())
                        .param("roomId", roomId)
                        .param("sender", buyer.getNickname())
                        .param("message", "사진 보냅니다."))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }

    @Test
    @DisplayName("18. 이미지가 포함된 메시지 조회 시 ImageUrls 필드 검증")
    void t18() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());

        MockMultipartFile imageFile = new MockMultipartFile(
                "images",
                "test-image.png",
                "image/png",
                "test content".getBytes()
        );

        sendMessage(roomId, buyer.getNickname(), "포토 메시지", imageFile);

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].message").value("포토 메시지"))
                .andExpect(jsonPath("$.data[0].imageUrls").isArray())
                .andExpect(jsonPath("$.data[0].imageUrls").isNotEmpty());
    }

    // --- [헬퍼 메서드] ---
    // 채팅방 생성 후 Room ID(UUID String) 반환
    private String createRoom(int itemId, String txType, String buyerApiKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(itemId))
                        .param("txType", txType)
                        .param("buyerApiKey", buyerApiKey))
                .andExpect(status().isOk())
                .andReturn();

        String jsonStr = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode root = objectMapper.readTree(jsonStr);
        return root.path("data").path("roomId").asText();
    }

    // 메시지 전송 (텍스트 + 선택적 이미지)
    private void sendMessage(String roomId, String sender, String message, MockMultipartFile imageFile) throws Exception {
        var requestBuilder = multipart("/api/chat/send")
                .with(csrf())
                .param("roomId", roomId)
                .param("sender", sender)
                .param("message", message);

        if (imageFile != null) {
            requestBuilder.file(imageFile);
        }

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));
    }
}