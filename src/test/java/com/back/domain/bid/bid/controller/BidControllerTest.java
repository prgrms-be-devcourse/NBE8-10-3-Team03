package com.back.domain.bid.bid.controller;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.domain.bid.bid.entity.Bid;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.member.member.entity.Member;
import com.back.global.rq.Rq;
import com.back.global.rsData.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private Rq rq;

    @MockitoBean
    private BidService bidService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;


    @Test
    @DisplayName("1. 입찰이 성공하면 HTTP 응답과 함께 웹소켓으로 메시지가 전송되어야 한다")
    @WithMockUser(username = "user1")
    void t1() throws Exception {
        Integer auctionId = 1;
        Integer bidPrice = 50000;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(bidPrice);

        Member mockActor = mock(Member.class);
        when(mockActor.getId()).thenReturn(1);
        when(mockActor.getNickname()).thenReturn("user1");

        when(rq.getActor()).thenReturn(mockActor);

        Bid mockBid = mock(Bid.class);
        Auction mockAuction = mock(Auction.class);

        when(mockBid.getId()).thenReturn(100);
        when(mockBid.getAuction()).thenReturn(mockAuction);
        when(mockAuction.getId()).thenReturn(auctionId);
        when(mockBid.getBidder()).thenReturn(mockActor); // 입찰자 설정
        when(mockBid.getPrice()).thenReturn(bidPrice);
        when(mockBid.getCreatedAt()).thenReturn(LocalDateTime.now());

        BidResponse mockResponse = new BidResponse(mockBid, bidPrice, 1, false);
        RsData<BidResponse> rsData = new RsData<>("200-1", "입찰 성공", mockResponse);

        when(bidService.createBid(anyInt(), any(BidCreateRequest.class), anyInt()))
                .thenReturn(rsData);

        ResultActions resultActions = mvc
                .perform(post("/api/auctions/" + auctionId + "/bids")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print());

        // HTTP 응답 검증
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.bidId").value(100))
                .andExpect(jsonPath("$.data.price").value(bidPrice))
                .andExpect(jsonPath("$.data.bidderNickname").value("user1"))
                .andExpect(jsonPath("$.data.currentHighestBid").value(bidPrice));

        // 웹소켓 브로드캐스팅 호출 여부 검증
        verify(messagingTemplate).convertAndSend(
                eq("/sub/auctions/" + auctionId),
                any(RsData.class)
        );
    }

    @Test
    @DisplayName("2. 입찰 서비스 실패(예: 금액 부족) 시 WebSocket 알림이 전송되지 않아야 한다")
    @WithMockUser(username = "user1")
    void t2() throws Exception {
        // Given
        int auctionId = 1;
        int bidPrice = 500; // 낮은 금액
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(bidPrice);

        Member mockMember = mock(Member.class);
        when(mockMember.getId()).thenReturn(1);
        when(rq.getActor()).thenReturn(mockMember);

        // 서비스가 실패 응답(400-1)을 리턴한다고 가정
        RsData<BidResponse> failResponse = new RsData<>("400-1", "입찰 금액이 현재가보다 낮습니다.", null);
        when(bidService.createBid(anyInt(), any(BidCreateRequest.class), anyInt()))
                .thenReturn(failResponse);

        // When
        mvc.perform(post("/api/auctions/" + auctionId + "/bids")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // [수정] 실제 응답인 400 Bad Request를 검증해야 함
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-1"));

        // Then
        verify(messagingTemplate, never()).convertAndSend(anyString(), (Object) any());
    }
}