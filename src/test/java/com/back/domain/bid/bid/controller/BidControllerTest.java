package com.back.domain.bid.bid.controller;

import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.enums.Role;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import com.back.global.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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

    @MockitoBean
    private BidService bidService;

    @MockitoBean
    private Rq rq;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final String AUTH_HEADER = "Bearer valid-token";


    @Test
    @DisplayName("1. 입찰 생성 성공 시 200 OK와 WebSocket 알림이 전송되어야 한다")
    void t1() throws Exception {
        int auctionId = 1;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(10000);

        Member actor = new Member(1, "user1", "홍길동", Role.USER);

        when(rq.getActor()).thenReturn(actor);

        BidResponse mockBidResponse = mock(BidResponse.class);
        RsData<BidResponse> successResponse = new RsData<>("200-1", "입찰이 완료되었습니다.", mockBidResponse);

        when(bidService.createBid(eq(auctionId), any(BidCreateRequest.class), eq(actor.getId())))
                .thenReturn(successResponse);

        mvc.perform(post("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .with(user(makeSecurityUser(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/sub/v1/auctions/" + auctionId),
                any(RsData.class)
        );
    }

    @Test
    @DisplayName("2. 입찰 서비스 실패 시(예: 금액 부족) WebSocket 알림이 전송되지 않아야 한다")
    void t2() throws Exception {
        int auctionId = 1;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(500);

        Member actor = new Member(1, "user1", "홍길동", Role.USER);
        when(rq.getActor()).thenReturn(actor);

        RsData<BidResponse> failResponse = new RsData<>("400-1", "입찰 금액이 낮습니다.", null);

        when(bidService.createBid(eq(auctionId), any(BidCreateRequest.class), eq(actor.getId())))
                .thenReturn(failResponse);


        mvc.perform(post("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .with(user(makeSecurityUser(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(RsData.class));
    }

    @Test
    @DisplayName("3. 입찰 생성 - 즉시구매가로 입찰 시 경매 즉시 종료")
    void t3() throws Exception {
        int auctionId = 1;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(100000); // 즉시구매가

        Member actor = new Member(2, "user2", "김철수", Role.USER);
        when(rq.getActor()).thenReturn(actor);

        BidResponse mockBidResponse = mock(BidResponse.class);
        RsData<BidResponse> successResponse = new RsData<>("200-1",
                "즉시구매가로 입찰되어 경매가 종료되었습니다.", mockBidResponse);

        when(bidService.createBid(eq(auctionId), any(BidCreateRequest.class), eq(actor.getId())))
                .thenReturn(successResponse);


        mvc.perform(post("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .with(user(makeSecurityUser(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"));

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/sub/v1/auctions/" + auctionId),
                any(RsData.class)
        );
    }

    @Test
    @DisplayName("4. 입찰 생성 실패 - 자신의 경매에 입찰")
    void t4() throws Exception {
        int auctionId = 1;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(50000);

        Member actor = new Member(1, "user1", "홍길동", Role.USER);
        when(rq.getActor()).thenReturn(actor);

        RsData<BidResponse> failResponse = new RsData<>("403-1",
                "자신의 경매에는 입찰할 수 없습니다.", null);

        when(bidService.createBid(eq(auctionId), any(BidCreateRequest.class), eq(actor.getId())))
                .thenReturn(failResponse);


        mvc.perform(post("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .with(user(makeSecurityUser(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(RsData.class));
    }

    @Test
    @DisplayName("5. 입찰 생성 실패 - 존재하지 않는 경매")
    void t5() throws Exception {
        int invalidAuctionId = 99999;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(50000);

        Member actor = new Member(2, "user2", "김철수", Role.USER);
        when(rq.getActor()).thenReturn(actor);

        RsData<BidResponse> failResponse = new RsData<>("404-1",
                "존재하지 않는 경매입니다.", null);

        when(bidService.createBid(eq(invalidAuctionId), any(BidCreateRequest.class), eq(actor.getId())))
                .thenReturn(failResponse);


        mvc.perform(post("/api/v1/auctions/" + invalidAuctionId + "/bids")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .with(user(makeSecurityUser(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(RsData.class));
    }

    @Test
    @DisplayName("6. 입찰 생성 실패 - 연속 입찰 시도")
    void t6() throws Exception {
        int auctionId = 1;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(60000);

        Member actor = new Member(2, "user2", "김철수", Role.USER);
        when(rq.getActor()).thenReturn(actor);

        RsData<BidResponse> failResponse = new RsData<>("400-3",
                "연속으로 입찰할 수 없습니다.", null);

        when(bidService.createBid(eq(auctionId), any(BidCreateRequest.class), eq(actor.getId())))
                .thenReturn(failResponse);


        mvc.perform(post("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .with(user(makeSecurityUser(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-3"));

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(RsData.class));
    }

    @Test
    @DisplayName("7. 입찰 생성 실패 - 최고 입찰가 기준 50% 초과")
    void t7() throws Exception {
        int auctionId = 1;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(200000); // 현재가의 50% 초과

        Member actor = new Member(2, "user2", "김철수", Role.USER);
        when(rq.getActor()).thenReturn(actor);

        RsData<BidResponse> failResponse = new RsData<>("400-4",
                "입찰가는 현재 최고가의 50%를 초과할 수 없습니다.", null);

        when(bidService.createBid(eq(auctionId), any(BidCreateRequest.class), eq(actor.getId())))
                .thenReturn(failResponse);


        mvc.perform(post("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER)
                        .with(csrf())
                        .with(user(makeSecurityUser(actor)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-4"));

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(RsData.class));
    }

    @Test
    @DisplayName("8. 특정 경매의 입찰 목록 조회 성공")
    void t8() throws Exception {
        int auctionId = 1;


        mvc.perform(get("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(handler().handlerType(BidController.class))
                .andExpect(handler().methodName("getBidsByAuction"))
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("9. 입찰 목록 조회 - 입찰 없는 경매")
    void t9() throws Exception {
        int auctionId = 999; // 입찰이 없는 경매


        mvc.perform(get("/api/v1/auctions/" + auctionId + "/bids")
                        .header("Authorization", AUTH_HEADER))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // SecurityUser 생성을 위한 헬퍼 메서드
    private SecurityUser makeSecurityUser(Member member) {
        return new SecurityUser(
                member.getId(),
                member.getUsername(),
                "",
                member.getNickname(),
                member.getRole(),
                List.of()
        );
    }
}