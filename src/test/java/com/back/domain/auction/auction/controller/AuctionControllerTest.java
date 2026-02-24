package com.back.domain.auction.auction.controller;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    private AuctionRepository auctionRepository;
    @Autowired
    private MockMvc mvc;

    private int findAuctionIdBySeller(String username) {
        return auctionRepository.findAll().stream()
                .filter(auction -> auction.getSeller().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("seller auction not found: " + username))
                .getId();
    }

    private int findAuctionIdBySellerAndBidCount(String username, int bidCount) {
        return auctionRepository.findAll().stream()
                .filter(auction -> auction.getSeller().getUsername().equals(username))
                .filter(auction -> auction.getBidCount() == bidCount)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("seller auction not found: " + username))
                .getId();
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 성공")
    void t1() throws Exception {
        
        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/auctions")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andDo(print());


        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("getAuctions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 - 카테고리 필터링")
    void t2() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/auctions")
                                .param("category", "디지털기기")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andDo(print());


        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 - 상태 필터링")
    void t3() throws Exception {

        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/auctions")
                                .param("status", "OPEN")
                                .param("page", "0")
                                .param("size", "20")
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 상세 조회 성공")
    void t4() throws Exception {
        int auctionId = 1;


        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/auctions/{auctionId}", auctionId)
                )
                .andDo(print());


        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("getAuctionDetail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매 상세 조회 성공"))
                .andExpect(jsonPath("$.data.auctionId").value(auctionId))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.seller").exists());
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("존재하지 않는 경매 조회 시 404")
    void t5() throws Exception {
        int invalidAuctionId = 99999;


        ResultActions resultActions = mvc
                .perform(
                        get("/api/v1/auctions/{auctionId}", invalidAuctionId)
                )
                .andDo(print());


        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 성공")
    void t6() throws Exception {

        MockMultipartFile request = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                    "name": "테스트 경매 상품",
                    "description": "테스트 설명",
                    "startPrice": 10000,
                    "buyNowPrice": 50000,
                    "categoryId": 1,
                    "durationHours": 168
                }
                """.getBytes()
        );


        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/auctions")
                                .file(request)
                                .with(csrf())
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());


        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("createAuction"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultCode").value("201-1"))
                .andExpect(jsonPath("$.msg").value("경매 물품이 등록되었습니다."))
                .andExpect(jsonPath("$.data.auctionId").exists());
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 실패 - 시작가가 즉시구매가보다 높음")
    void t7() throws Exception {

        MockMultipartFile request = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                    "name": "테스트 경매 상품",
                    "description": "테스트 설명",
                    "startPrice": 100000,
                    "buyNowPrice": 50000,
                    "categoryId": 1,
                    "durationHours": 168
                }
                """.getBytes()
        );


        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/auctions")
                                .file(request)
                                .with(csrf())
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());


        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resultCode").value("400-2"));
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 수정 성공 - 입찰 전")
    void t8() throws Exception {
        int auctionId = findAuctionIdBySellerAndBidCount("user1", 0);


        MockMultipartFile request = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                """
                {
                    "name": "수정된 경매 상품",
                    "description": "수정된 설명"
                }
                """.getBytes()
        );


        ResultActions resultActions = mvc
                .perform(
                        multipart("/api/v1/auctions/{auctionId}", auctionId)
                                .file(request)
                                .with(csrf())
                                .with(request1 -> {
                                    request1.setMethod("PATCH");
                                    return request1;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                )
                .andDo(print());


        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("updateAuction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매 물품이 수정되었습니다."));
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 삭제 성공 - 입찰 없음")
    void t9() throws Exception {
        int auctionId = findAuctionIdBySellerAndBidCount("user1", 0);


        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/auctions/{auctionId}", auctionId)
                                .with(csrf())
                )
                .andDo(print());


        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("deleteAuction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."));
    }

    @Test
    @WithUserDetails("user2")
    @DisplayName("경매 삭제 실패 - 권한 없음 (다른 사용자의 경매)")
    void t10() throws Exception {
        int auctionId = findAuctionIdBySellerAndBidCount("user1", 0);


        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/auctions/{auctionId}", auctionId)
                                .with(csrf())
                )
                .andDo(print());


        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.resultCode").value("403-1"));
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 삭제 실패 - 존재하지 않는 경매")
    void t11() throws Exception {
        int invalidAuctionId = 99999;


        ResultActions resultActions = mvc
                .perform(
                        delete("/api/v1/auctions/{auctionId}", invalidAuctionId)
                                .with(csrf())
                )
                .andDo(print());


        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.resultCode").value("404-1"));
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 거래 취소 성공 - 판매자")
    void t12() throws Exception {
        int auctionId = findAuctionIdBySeller("user1");
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        Member winner = memberService.findByUsername("user2").orElseThrow();
        auction.completeWithWinner(winner.getId());
        auctionRepository.save(auction);


        ResultActions resultActions = mvc
                .perform(
                        post("/api/v1/auctions/{auctionId}/cancel", auctionId)
                                .with(csrf())
                )
                .andDo(print());


        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("cancelTrade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("입찰 O -> 경매 취소 시 신용도 감소")
    void t13() throws Exception {
        int auctionId = findAuctionIdBySellerAndBidCount("user1", 0);
        Auction auction = auctionRepository.findById(auctionId).orElseThrow();
        auction.updateBid(auction.getStartPrice() + 1000);
        auctionRepository.save(auction);

        double beforeScore = memberService.findByUsername("user1").orElseThrow().getReputation().getScore();

        ResultActions resultActions = mvc
                .perform(
                                delete("/api/v1/auctions/%d".formatted(auctionId))
                                        .with(csrf())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("deleteAuction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."));

        double afterScore = memberService.findByUsername("user1").orElseThrow().getReputation().getScore();
        assertThat(afterScore).isLessThan(beforeScore);
    }

    @Test
    @WithUserDetails("user2")
    @DisplayName("입찰 X -> 경매 취소 시 신용도 감소 X")
    void t14() throws Exception {
        int auctionId = findAuctionIdBySellerAndBidCount("user2", 0);
        double beforeScore = memberService.findByUsername("user2").orElseThrow().getReputation().getScore();

        ResultActions resultActions = mvc
                .perform(
                                delete("/api/v1/auctions/%d".formatted(auctionId))
                                        .with(csrf())
                )
                .andDo(print());

        resultActions
                .andExpect(handler().handlerType(AuctionController.class))
                .andExpect(handler().methodName("deleteAuction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200-1"))
                .andExpect(jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."));

        double afterScore = memberService.findByUsername("user2").orElseThrow().getReputation().getScore();
        assertThat(afterScore).isEqualTo(beforeScore);
    }
}
