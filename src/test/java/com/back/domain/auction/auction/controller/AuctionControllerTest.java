package com.back.domain.auction.auction.controller;

import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuctionControllerTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MockMvc mvc;

    @Test
    @WithUserDetails("user1")
    @DisplayName("입찰 O -> 경매 취소 시 신용도 감소")
    void t13() throws Exception {
        Member user1 = memberService.findByUsername("user1").get();
        int userId = user1.getId();
        int auctionId = 1;

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/auctions/%d".formatted(auctionId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("deleteAuction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."));

        assertThat(user1.getReputation().getScore()).isEqualTo(45.0);
    }

    @Test
    @WithUserDetails("user2")
    @DisplayName("입찰 X -> 경매 취소 시 신용도 감소 X")
    void t14() throws Exception {
        Member user2 = memberService.findByUsername("user2").get();
        int auctionId = 2;

        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/auctions/%d".formatted(auctionId))
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("deleteAuction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."));

        assertThat(user2.getReputation().getScore()).isEqualTo(50.0);
    }
}
