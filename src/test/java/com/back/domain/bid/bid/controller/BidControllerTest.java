package com.back.domain.bid.bid.controller;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.domain.bid.bid.entity.Bid;
import com.back.domain.bid.bid.service.BidService;
import com.back.domain.member.member.entity.Member;
import com.back.global.rsData.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
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
    private BidService bidService;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("입찰이 성공하면 HTTP 응답과 함께 웹소켓으로 메시지가 전송되어야 한다")
    @WithUserDetails("user1")
    void t1() throws Exception {

        Integer auctionId = 1;
        Integer bidPrice = 50000;
        BidCreateRequest request = new BidCreateRequest();
        request.setPrice(bidPrice);

        Bid mockBid = mock(Bid.class);
        Auction mockAuction = mock(Auction.class);
        Member mockBidder = mock(Member.class);

        when(mockBid.getId()).thenReturn(100);
        when(mockBid.getAuction()).thenReturn(mockAuction);
        when(mockAuction.getId()).thenReturn(auctionId);
        when(mockBid.getBidder()).thenReturn(mockBidder);
        when(mockBidder.getId()).thenReturn(1);
        when(mockBidder.getNickname()).thenReturn("user1");
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
}