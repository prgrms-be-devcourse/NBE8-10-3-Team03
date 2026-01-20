package com.back.domain.chat.chat.controller;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.chat.chat.dto.ChatDto;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.Role;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.post.post.entity.Post;
import com.back.domain.post.post.entity.PostStatus;
import com.back.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isNotEmpty());
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
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isNotEmpty());
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
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
                .andExpect(jsonPath("$.resultCode").value("400-2"))
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
                .andExpect(jsonPath("$.resultCode").value("400-1"))
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
                .andExpect(status().isBadRequest()) // 400 응답 확인
                .andExpect(jsonPath("$.resultCode").value("400-1"));
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
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("본인의 상품에는 채팅을 개설할 수 없습니다."));
    }

    @Test
    @DisplayName("10. 메시지 전송 성공")
    void t10() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        ChatDto dto = new ChatDto(0, salePostId, roomId, buyer.getNickname(), "구매 희망합니다.", null, false);

        mockMvc.perform(post("/api/chat/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("11. 존재하지 않는 방 ID로 메시지 전송 시 실패")
    void t11() throws Exception {
        ChatDto dto = new ChatDto(0, salePostId, "invalid-room-uuid", buyer.getNickname(), "Hello", null, false);

        mockMvc.perform(post("/api/chat/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 채팅방입니다."));
    }

    @Test
    @DisplayName("12. 메시지 목록 조회 및 내용 검증")
    void t12() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, salePostId, buyer.getNickname(), "안녕하세요");
        sendMessage(roomId, salePostId, seller.getNickname(), "반갑습니다");

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].message").value("안녕하세요"))
                .andExpect(jsonPath("$[1].message").value("반갑습니다"));
    }

    @Test
    @DisplayName("13. 읽음 처리 확인 (상대방이 조회하면 isRead가 true)")
    void t13() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, salePostId, seller.getNickname(), "메시지");

        mockMvc.perform(get("/api/chat/room/" + roomId)
                        .param("readerName", buyer.getNickname()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(true));
    }

    @Test
    @DisplayName("14. lastChatId를 이용한 페이징(이어보기)")
    void t14() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, salePostId, buyer.getNickname(), "1");
        sendMessage(roomId, salePostId, buyer.getNickname(), "2");
        sendMessage(roomId, salePostId, buyer.getNickname(), "3");

        MvcResult result = mockMvc.perform(get("/api/chat/room/" + roomId)).andReturn();
        String content = result.getResponse().getContentAsString();
        ChatDto[] chats = objectMapper.readValue(content, ChatDto[].class);
        int secondMsgId = chats[1].id();

        mockMvc.perform(get("/api/chat/room/" + roomId)
                        .param("lastChatId", String.valueOf(secondMsgId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("3"));
    }

    @Test
    @DisplayName("15. 채팅 목록 조회 (여러 방이 있을 때)")
    void t15() throws Exception {
        String room1 = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(room1, salePostId, buyer.getNickname(), "Room1 Msg");

        String room2 = createRoom(salePostId, "POST", anotherUser.getApiKey());
        sendMessage(room2, salePostId, anotherUser.getNickname(), "Room2 Msg");

        mockMvc.perform(get("/api/chat/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    assertThat(json).contains("Room1 Msg");
                    assertThat(json).contains("Room2 Msg");
                });
    }

    @Test
    @DisplayName("16. 채팅 목록 갱신 확인 (최신 메시지 반영)")
    void t16() throws Exception {
        String roomId = createRoom(salePostId, "POST", buyer.getApiKey());
        sendMessage(roomId, salePostId, buyer.getNickname(), "Old");
        sendMessage(roomId, salePostId, buyer.getNickname(), "New");

        mockMvc.perform(get("/api/chat/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("New"));
    }

    // --- [헬퍼 메서드] ---
    // 채팅방 생성 API를 호출하고 생성된 UUID(String)를 반환
    private String createRoom(int itemId, String txType, String buyerApiKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat/room")
                        .with(csrf())
                        .param("itemId", String.valueOf(itemId))
                        .param("txType", txType)
                        .param("buyerApiKey", buyerApiKey))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private void sendMessage(String roomId, int itemId, String sender, String message) throws Exception {
        ChatDto dto = new ChatDto(0, itemId, roomId, sender, message, LocalDateTime.now(), false);
        mockMvc.perform(post("/api/chat/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}