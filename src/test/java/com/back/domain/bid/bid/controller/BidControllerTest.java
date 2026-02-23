package com.back.domain.bid.bid.controller;

import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.global.rsData.RsData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BidControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoSpyBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeEach
    void setUp() {
        reset(messagingTemplate);
    }

    @Test
    @DisplayName("1. 입찰 생성 성공")
    void t1() throws Exception {
        int auctionId = 1; // seller: user2, startPrice: 11,000

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":12000}
                                """))
                .andDo(print())
                .andExpect(handler().handlerType(BidController.class))
                .andExpect(handler().methodName("createBid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.price").value(12000))
                .andExpect(jsonPath("$.data.auctionId").value(auctionId))
                .andExpect(jsonPath("$.data.buyNow").value(false));

        assertThat(uniqueBroadcastCount(auctionId)).isEqualTo(1);
        assertThat(payloadsForAuction(auctionId).stream()
                .anyMatch(payload -> payload instanceof RsData<?> rsData && "200-1".equals(rsData.resultCode()))
        ).isTrue();
    }

    @Test
    @DisplayName("2. 입찰 생성 실패 - 입찰가 부족")
    void t2() throws Exception {
        int auctionId = 2; // startPrice: 12,000

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":12000}
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));

        assertThat(uniqueBroadcastCount(auctionId)).isZero();
    }

    @Test
    @DisplayName("3. 입찰 생성 - 즉시구매가로 입찰 시 경매 즉시 종료")
    void t3() throws Exception {
        int auctionId = 1; // startPrice: 11,000, buyNowPrice: 22,000

        // 현재가를 먼저 올려야 buyNow 입찰(22,000)이 150% 제한에 걸리지 않는다.
        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":16000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":22000}
                                """))
                .andDo(print())
                .andExpect(handler().handlerType(BidController.class))
                .andExpect(handler().methodName("createBid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.price").value(22000))
                .andExpect(jsonPath("$.data.buyNow").value(true));

        assertThat(uniqueBroadcastCount(auctionId)).isEqualTo(2);

        assertThat(payloadsForAuction(auctionId).stream()
                .filter(payload -> payload instanceof RsData<?>)
                .map(payload -> ((RsData<?>) payload).data())
                .anyMatch(data -> data instanceof BidResponse bidResponse && bidResponse.isBuyNow())
        ).isTrue();
    }

    @Test
    @DisplayName("4. 입찰 생성 실패 - 자신의 경매에 입찰")
    void t4() throws Exception {
        int auctionId = 5; // seller: user1

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":16000}
                                """))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));

        assertThat(uniqueBroadcastCount(auctionId)).isZero();
    }

    @Test
    @DisplayName("5. 입찰 생성 실패 - 존재하지 않는 경매")
    void t5() throws Exception {
        int invalidAuctionId = 99999;

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", invalidAuctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":50000}
                                """))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));

        assertThat(uniqueBroadcastCount(invalidAuctionId)).isZero();
    }

    @Test
    @DisplayName("6. 입찰 생성 실패 - 연속 입찰 시도")
    void t6() throws Exception {
        int auctionId = 2;

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":13000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":14000}
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-6"));

        assertThat(uniqueBroadcastCount(auctionId)).isEqualTo(1);
    }

    @Test
    @DisplayName("7. 입찰 생성 실패 - 최고 입찰가 기준 50% 초과")
    void t7() throws Exception {
        int auctionId = 3; // startPrice: 13,000, maxAllowed: 19,500

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":20000}
                                """))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));

        assertThat(uniqueBroadcastCount(auctionId)).isZero();
    }

    @Test
    @DisplayName("8. 특정 경매의 입찰 목록 조회 성공")
    void t8() throws Exception {
        int auctionId = 4;

        mvc.perform(post("/api/v1/auctions/{auctionId}/bids", auctionId)
                        .header("Authorization", bearer("user1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"price":15000}
                                """))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/auctions/{auctionId}/bids", auctionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(BidController.class))
                .andExpect(handler().methodName("getBids"))
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].price").value(15000));
    }

    @Test
    @DisplayName("9. 입찰 목록 조회 - 입찰 없는 경매")
    void t9() throws Exception {
        int auctionId = 6; // 존재하는 경매, 기본 데이터에서 입찰 없음

        ResultActions resultActions = mvc.perform(get("/api/v1/auctions/{auctionId}/bids", auctionId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    private String bearer(String apiKey) {
        return "Bearer " + apiKey;
    }

    private long uniqueBroadcastCount(int auctionId) {
        Set<Object> uniquePayloads = Collections.newSetFromMap(new IdentityHashMap<>());
        uniquePayloads.addAll(payloadsForAuction(auctionId));
        return uniquePayloads.size();
    }

    private List<Object> payloadsForAuction(int auctionId) {
        String destination = "/sub/v1/auctions/" + auctionId;
        return mockingDetails(messagingTemplate).getInvocations().stream()
                .filter(invocation -> "convertAndSend".equals(invocation.getMethod().getName()))
                .filter(invocation -> invocation.getArguments().length >= 2)
                .filter(invocation -> destination.equals(invocation.getArguments()[0]))
                .map(invocation -> invocation.getArguments()[1])
                .toList();
    }
}
