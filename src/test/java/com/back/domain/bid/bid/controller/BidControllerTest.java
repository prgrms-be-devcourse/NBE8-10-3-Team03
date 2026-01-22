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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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

        // When & Then
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