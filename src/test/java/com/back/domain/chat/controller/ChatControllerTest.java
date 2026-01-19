package com.back.domain.chat.controller;

import com.back.domain.chat.dto.ChatDto;
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

    private final String ROOM_ID = UUID.randomUUID().toString();

    @Test
    @DisplayName("판매자와 구매자가 정상적으로 대화를 주고받고 저장된다.")
    void t1() throws Exception {
        sendMessage(BUYER, "이 물건 네고 가능한가요?");
        sendMessage(SELLER, "아뇨, 정가 판매만 합니다.");
        sendMessage(BUYER, "네, 알겠습니다. 구매할게요.");

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].sender").value(BUYER))
                .andExpect(jsonPath("$[1].sender").value(SELLER))
                .andExpect(jsonPath("$[2].sender").value(BUYER));
    }

    @Test
    @DisplayName("다른 UUID 방의 메시지는 현재 방에 조회되지 않아야 한다.")
    void t2() throws Exception {
        sendMessage(BUYER, "A가 보낸 메시지 (ROOM_ID)");

        String otherRoomId = UUID.randomUUID().toString();
        sendMessageRaw(ITEM_ID, otherRoomId, "buyer_park", "B가 보낸 메시지 (otherRoomId)");

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("A가 보낸 메시지 (ROOM_ID)"));
    }

    @Test
    @DisplayName("존재하지 않거나 메시지가 없는 방을 폴링하면 빈 리스트를 응답한다.")
    void t3() throws Exception {
        mockMvc.perform(get("/api/chat/room/" + UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("메시지 내용이 극단적으로 길거나 특수문자가 포함되어도 처리 가능하다.")
    void t4() throws Exception {
        String longAndSpecial = "장문 메시지 테스트: " + "ㅋ".repeat(1000) + " 😊❤️ @#$%";
        sendMessage(BUYER, longAndSpecial);

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
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
    @DisplayName("0.01초 간격으로 메시지가 쌓여도 시간 순서대로 정렬되어야 한다.")
    void t6() throws Exception {
        for (int i = 1; i <= 5; i++) {
            sendMessage(BUYER, "메시지 " + i);
            Thread.sleep(10); // 0.01초 대기
        }

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(jsonPath("$[0].message").value("메시지 1"))
                .andExpect(jsonPath("$[4].message").value("메시지 5"));
    }

    @Test
    @DisplayName("동일한 참여자라도 상품(itemId)이 다르면 방 ID(UUID)가 달라야 한다.")
    void t7() throws Exception {
        Long itemA = 100L;
        Long itemB = 200L;

        // 클라이언트에서 별도의 UUID를 생성해서 보낸다고 가정
        String roomA = UUID.randomUUID().toString();
        String roomB = UUID.randomUUID().toString();

        sendMessageRaw(itemA, roomA, BUYER, "아이템A 문의");
        sendMessageRaw(itemB, roomB, BUYER, "아이템B 문의");

        // 각각의 방에 메시지가 1개씩만 존재해야 함
        mockMvc.perform(get("/api/chat/room/" + roomA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("아이템A 문의"));

        mockMvc.perform(get("/api/chat/room/" + roomB))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("아이템B 문의"));
    }

    @Test
    @DisplayName("실시간 채팅 시나리오: 조회 -> 메시지 추가 -> 다시 조회")
    void t8() throws Exception {
        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        sendMessage(BUYER, "첫 번째 질문입니다.");

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("첫 번째 질문입니다."));

        sendMessage(SELLER, "답변 드립니다.");

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].message").value("답변 드립니다."));
    }

    @Test
    @DisplayName("lastChatId를 전달하면 해당 ID 이후의 메시지만 응답한다 (무한 스크롤/폴링 최적화).")
    void t9() throws Exception {
        sendMessage(BUYER, "메시지 1");
        sendMessage(SELLER, "메시지 2");
        sendMessage(BUYER, "메시지 3");

        MvcResult result = mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andReturn();
        String content = result.getResponse().getContentAsString();
        Integer firstMessageId = com.jayway.jsonpath.JsonPath.read(content, "$[0].id");

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID)
                        .param("lastChatId", String.valueOf(firstMessageId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].message").value("메시지 2"))
                .andExpect(jsonPath("$[1].message").value("메시지 3"));
    }

    @Test
    @DisplayName("채팅 목록 조회 시 각 방의 '가장 최신 메시지' 하나씩만 보여준다.")
    void t10() throws Exception {
        // Room 1
        String room1 = UUID.randomUUID().toString();
        sendMessageRaw(100L, room1, "buyer1", "안녕하세요");
        sendMessageRaw(100L, room1, "seller", "반갑습니다"); // 최신

        // Room 2
        String room2 = UUID.randomUUID().toString();
        sendMessageRaw(200L, room2, "buyer2", "네고 되나요?");
        sendMessageRaw(200L, room2, "seller", "안됩니다");
        sendMessageRaw(200L, room2, "buyer2", "쳇"); // 최신

        mockMvc.perform(get("/api/chat/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // UUID라 순서 보장이 어려우므로, 내용과 roomId가 매칭되는지 확인
                .andExpect(jsonPath("$[?(@.message == '반갑습니다')].roomId").exists())
                .andExpect(jsonPath("$[?(@.message == '쳇')].roomId").exists());
    }

    @Test
    @DisplayName("상대방이 메시지를 읽으면(조회하면) isRead가 true로 변해야 한다.")
    void t11() throws Exception {
        sendMessage(SELLER, "안 팔아요.");

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID)
                        .param("readerName", BUYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(true));

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID)
                        .param("readerName", SELLER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(true));
    }

    // --- [헬퍼 메서드] ---

    private void sendMessage(String sender, String message) throws Exception {
        sendMessageRaw(ITEM_ID, ROOM_ID, sender, message);
    }

    private void sendMessageRaw(Long itemId, String roomId, String sender, String message) throws Exception {
        // ChatDto: id, itemId, roomId, sender, message, createDate, isRead
        ChatDto dto = new ChatDto(0, itemId, roomId, sender, message, null, false);

        mockMvc.perform(post("/api/chat/send")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}