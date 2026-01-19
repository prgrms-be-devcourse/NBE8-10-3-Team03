package com.back.api.chat;

import com.back.domain.chat.dto.ChatDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private final Long ITEM_ID = 100L;
    private final String SELLER = "seller_kim";
    private final String BUYER = "buyer_lee";
    private final String ROOM_ID = "100-seller_kim-buyer_lee";

    @Test
    @DisplayName("판매자와 구매자가 정상적으로 대화를 주고받는다.")
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
    @DisplayName("특정 상품의 다른 구매자와의 대화방은 완전히 분리되어야 한다.")
    void t2() throws Exception {
        sendMessage(BUYER, "A가 보낸 메시지");

        String otherRoomId = "100-seller_kim-buyer_park";
        ChatDto otherDto = new ChatDto(ITEM_ID, otherRoomId, "buyer_park", "B가 보낸 메시지", null);
        mockMvc.perform(post("/api/chat/send").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherDto)));

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].message").value("A가 보낸 메시지"));
    }

    @Test
    @DisplayName("메시지가 없는 방을 폴링하면 빈 리스트를 응답한다.")
    void t3() throws Exception {
        mockMvc.perform(get("/api/chat/room/empty-room-id"))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(brokenJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("0.01초 간격으로 메시지가 쌓여도 시간 순서대로 정렬되어야 한다.")
    void t6() throws Exception {
        for(int i=1; i<=5; i++) {
            sendMessage(BUYER, "메시지 " + i);
            Thread.sleep(10); // 미세한 시간차
        }

        mockMvc.perform(get("/api/chat/room/" + ROOM_ID))
                .andExpect(jsonPath("$[0].message").value("메시지 1"))
                .andExpect(jsonPath("$[4].message").value("메시지 5"));
    }

    @Test
    @DisplayName("동일한 참여자라도 상품(itemId)이 다르면 방 ID가 달라야 한다.")
    void t7() throws Exception {
        Long itemA = 100L;
        Long itemB = 200L;
        String roomA = String.format("%d-%s-%s", itemA, SELLER, BUYER);
        String roomB = String.format("%d-%s-%s", itemB, SELLER, BUYER);

        sendMessage(BUYER, "아이템A 문의");

        ChatDto dtoB = new ChatDto(itemB, roomB, BUYER, "아이템B 문의", null);
        mockMvc.perform(post("/api/chat/send").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dtoB)));

        mockMvc.perform(get("/api/chat/room/" + roomA)).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/chat/room/" + roomB)).andExpect(jsonPath("$.length()").value(1));
    }

    // --- [헬퍼 메서드] ---
    private void sendMessage(String sender, String message) throws Exception {
        ChatDto dto = new ChatDto(ITEM_ID, ROOM_ID, sender, message, null);
        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}