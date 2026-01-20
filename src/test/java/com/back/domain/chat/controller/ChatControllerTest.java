package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatDto;
import com.jayway.jsonpath.JsonPath;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    private final Long ITEM_ID = 100L;
    private final String SELLER = "seller_kim";
    private final String BUYER = "buyer_lee";

    @Test
    @DisplayName("판매자와 구매자가 정상적으로 대화를 주고받고 저장된다.")
    void t1() throws Exception {
        String roomId = createRoom(ITEM_ID, SELLER, BUYER);

        sendMessage(roomId, BUYER, "이 물건 네고 가능한가요?");
        sendMessage(roomId, SELLER, "안팔아요~");
        sendMessage(roomId, BUYER, "알겠습니다.그냥 구매할게요.");

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].sender").value(BUYER))
                .andExpect(jsonPath("$[1].sender").value(SELLER))
                .andExpect(jsonPath("$[2].sender").value(BUYER));
    }

    @Test
    @DisplayName("다른 UUID 방의 메시지는 현재 방에 조회되지 않아야 한다.")
    void t2() throws Exception {
        String roomId = createRoom(ITEM_ID, SELLER, BUYER);
        sendMessage(roomId, BUYER, "A가 보낸 메시지");

        // 다른 방 생성
        String otherRoomId = createRoom(200L, "otherSeller", "otherBuyer");
        sendMessage(otherRoomId, "otherBuyer", "B가 보낸 메시지");

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("A가 보낸 메시지"));
    }

    @Test
    @DisplayName("존재하지 않거나 메시지가 없는 방을 폴링하면 빈 리스트를 응답한다.")
    void t3() throws Exception {
        // 무작위 UUID로 존재하지 않는 방 조회
        mockMvc.perform(get("/api/chat/room/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("메시지 내용이 극단적으로 길거나 특수문자가 포함되어도 처리 가능하다.")
    void t4() throws Exception {
        String roomId = createRoom(ITEM_ID, SELLER, BUYER);
        String longAndSpecial = "장문 메시지 테스트: " + "ㅋ".repeat(1000) + " 😊❤️ @#$%";
        sendMessage(roomId, BUYER, longAndSpecial);

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value(longAndSpecial));
    }

    @Test
    @DisplayName("잘못된 JSON 형식으로 요청을 보내면 400 에러를 반환한다.")
    void t5() throws Exception {
        String brokenJson = "{ \"itemId\": 100, \"message\": ";

        mockMvc.perform(post("/api/chat/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brokenJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("순차적으로 메시지가 쌓여도 시간 순서대로 정렬되어야 한다.")
    void t6() throws Exception {
        String roomId = createRoom(ITEM_ID, SELLER, BUYER);

        for (int i = 1; i <= 5; i++) {
            sendMessage(roomId, BUYER, "메시지 " + i);
        }

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(jsonPath("$[0].message").value("메시지 1"))
                .andExpect(jsonPath("$[1].message").value("메시지 2"))
                .andExpect(jsonPath("$[2].message").value("메시지 3"))
                .andExpect(jsonPath("$[3].message").value("메시지 4"))
                .andExpect(jsonPath("$[4].message").value("메시지 5"));
    }


    @Test
    @DisplayName("동일한 참여자라도 상품(itemId)이 다르면 방 ID(UUID)가 달라야 한다.")
    void t7() throws Exception {
        String roomA = createRoom(100L, SELLER, BUYER);
        String roomB = createRoom(200L, SELLER, BUYER);

        // 두 UUID가 서로 달라야 함
        assert !roomA.equals(roomB);

        sendMessage(roomA, BUYER, "아이템A 문의");
        sendMessage(roomB, BUYER, "아이템B 문의");

        mockMvc.perform(get("/api/chat/room/" + roomA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("아이템A 문의"));
    }

    @Test
    @DisplayName("실시간 채팅 시나리오: 조회 -> 메시지 추가 -> 다시 조회")
    void t8() throws Exception {
        String roomId = createRoom(ITEM_ID, SELLER, BUYER);

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        sendMessage(roomId, BUYER, "첫 번째 질문입니다.");

        mockMvc.perform(get("/api/chat/room/" + roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("첫 번째 질문입니다."));
    }

    @Test
    @DisplayName("lastChatId를 전달하면 해당 ID 이후의 메시지만 응답한다.")
    void t9() throws Exception {
        String roomId = createRoom(ITEM_ID, SELLER, BUYER);
        sendMessage(roomId, BUYER, "메시지 1");
        sendMessage(roomId, SELLER, "메시지 2");
        sendMessage(roomId, BUYER, "메시지 3");

        MvcResult result = mockMvc.perform(get("/api/chat/room/" + roomId))
                .andReturn();
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Integer firstMessageId = JsonPath.read(content, "$[0].id");

        mockMvc.perform(get("/api/chat/room/" + roomId)
                        .param("lastChatId", String.valueOf(firstMessageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].message").value("메시지 2"))
                .andExpect(jsonPath("$[1].message").value("메시지 3"));
    }

    @Test
    @DisplayName("채팅 목록 조회 시 각 방의 '가장 최신 메시지' 하나씩만 보여준다.")
    void t10() throws Exception {
        String room1 = createRoom(100L, "seller1", "buyer1");
        sendMessage(room1, "buyer1", "안녕하세요");
        sendMessage(room1, "seller1", "반갑습니다");

        String room2 = createRoom(200L, "seller2", "buyer2");
        sendMessage(room2, "buyer2", "네고 되나요?");
        sendMessage(room2, "seller2", "퉷");

        mockMvc.perform(get("/api/chat/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.message == '반갑습니다')].roomId").value(room1))
                .andExpect(jsonPath("$[?(@.message == '퉷')].roomId").value(room2));
    }

    @Test
    @DisplayName("상대방이 메시지를 읽으면(조회하면) isRead가 true로 변해야 한다.")
    void t11() throws Exception {
        String roomId = createRoom(ITEM_ID, SELLER, BUYER);
        sendMessage(roomId, SELLER, "안 팔아요.");

        // 구매자가 읽음
        mockMvc.perform(get("/api/chat/room/" + roomId)
                        .param("readerName", BUYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(true));
    }

    // --- [헬퍼 메서드] ---
    // 채팅방 생성 API를 호출하고 생성된 UUID(String)를 반환
    private String createRoom(Long itemId, String seller, String buyer) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/chat/room")
                        .with(csrf()) // 보안 설정 때문에 csrf 토큰 필요
                        .param("itemId", String.valueOf(itemId))
                        .param("sellerId", seller)
                        .param("buyerId", buyer))
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getContentAsString();
    }

    private void sendMessage(String roomId, String sender, String message) throws Exception {
        ChatDto dto = new ChatDto(0, ITEM_ID, roomId, sender, message, null, false);

        mockMvc.perform(post("/api/chat/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}