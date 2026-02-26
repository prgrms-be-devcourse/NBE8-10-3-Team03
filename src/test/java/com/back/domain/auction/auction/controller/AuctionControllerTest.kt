package com.back.domain.auction.auction.controller

import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.member.member.service.MemberService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuctionControllerTest {
    @Autowired
    private lateinit var memberService: MemberService

    @Autowired
    private lateinit var auctionRepository: AuctionRepository

    @Autowired
    private lateinit var mvc: MockMvc

    private fun findAuctionIdBySeller(username: String): Int =
        auctionRepository.findAll().firstOrNull { it.seller.username == username }?.id
            ?: throw IllegalStateException("seller auction not found: $username")

    private fun findAuctionIdBySellerAndBidCount(username: String, bidCount: Int): Int =
        auctionRepository.findAll().firstOrNull { it.seller.username == username && it.bidCount == bidCount }?.id
            ?: throw IllegalStateException("seller auction not found: $username")

    private fun applyParams(
        builder: MockMultipartHttpServletRequestBuilder,
        params: Map<String, String>
    ): MockMultipartHttpServletRequestBuilder =
        params.entries.fold(builder) { acc, (key, value) -> acc.param(key, value) }

    private fun patchAuctionRequest(auctionId: Int, params: Map<String, String>) =
        applyParams(
            MockMvcRequestBuilders.multipart("/api/v1/auctions/{auctionId}", auctionId),
            params
        )
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .with(RequestPostProcessor { req: MockHttpServletRequest ->
                req.method = "PATCH"
                req
            })
            .contentType(MediaType.MULTIPART_FORM_DATA)

    private fun createAuctionRequest(params: Map<String, String>) =
        applyParams(
            MockMvcRequestBuilders.multipart("/api/v1/auctions"),
            params
        )
            .with(SecurityMockMvcRequestPostProcessors.csrf())
            .contentType(MediaType.MULTIPART_FORM_DATA)

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 성공")
    @Throws(Exception::class)
    fun t1() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/auctions")
                    .param("page", "0")
                    .param("size", "20")
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getAuctions"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("경매 목록 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 - 카테고리 필터링")
    @Throws(Exception::class)
    fun t2() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/auctions")
                    .param("category", "디지털기기")
                    .param("page", "0")
                    .param("size", "20")
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 - 상태 필터링")
    @Throws(Exception::class)
    fun t3() {
        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/auctions")
                    .param("status", "OPEN")
                    .param("page", "0")
                    .param("size", "20")
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 상세 조회 성공")
    @Throws(Exception::class)
    fun t4() {
        val auctionId = 1


        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}", auctionId)
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getAuctionDetail"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("경매 상세 조회 성공"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.auctionId").value(auctionId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.seller").exists())
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("존재하지 않는 경매 조회 시 404")
    @Throws(Exception::class)
    fun t5() {
        val invalidAuctionId = 99999


        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}", invalidAuctionId)
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 성공")
    @Throws(Exception::class)
    fun t6() {
        val request = mapOf(
            "name" to "테스트 경매 상품",
            "description" to "테스트 설명",
            "startPrice" to "10000",
            "buyNowPrice" to "50000",
            "categoryId" to "1",
            "durationHours" to "168"
        )


        val resultActions = mvc
            .perform(createAuctionRequest(request))
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("createAuction"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("201-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("경매 물품이 등록되었습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.auctionId").exists())
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 실패 - 시작가가 즉시구매가보다 높음")
    @Throws(Exception::class)
    fun t7() {
        val request = mapOf(
            "name" to "테스트 경매 상품",
            "description" to "테스트 설명",
            "startPrice" to "100000",
            "buyNowPrice" to "50000",
            "categoryId" to "1",
            "durationHours" to "168"
        )


        val resultActions = mvc
            .perform(createAuctionRequest(request))
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-2"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 수정 성공 - 입찰 전")
    @Throws(Exception::class)
    fun t8() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)


        val request = mapOf(
            "name" to "수정된 경매 상품",
            "description" to "수정된 설명"
        )


        val resultActions = mvc.perform(patchAuctionRequest(auctionId, request))
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("updateAuction"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("경매 물품이 수정되었습니다."))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 삭제 성공 - 입찰 없음")
    @Throws(Exception::class)
    fun t9() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)


        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/auctions/{auctionId}", auctionId)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deleteAuction"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."))
    }

    @Test
    @WithUserDetails("user2")
    @DisplayName("경매 삭제 실패 - 권한 없음 (다른 사용자의 경매)")
    @Throws(Exception::class)
    fun t10() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)


        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/auctions/{auctionId}", auctionId)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 삭제 실패 - 존재하지 않는 경매")
    @Throws(Exception::class)
    fun t11() {
        val invalidAuctionId = 99999


        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/auctions/{auctionId}", invalidAuctionId)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 거래 취소 성공 - 판매자")
    @Throws(Exception::class)
    fun t12() {
        val auctionId = findAuctionIdBySeller("user1")
        val auction = auctionRepository.findById(auctionId).orElseThrow()
        val winner = memberService.findByUsername("user2")!!
        auction.completeWithWinner(winner.id)
        auctionRepository.save(auction)


        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/cancel", auctionId)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
            )
            .andDo(MockMvcResultHandlers.print())


        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("cancelTrade"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").exists())
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("입찰 O -> 경매 취소 시 신용도 감소")
    @Throws(Exception::class)
    fun t13() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)
        val auction = auctionRepository.findById(auctionId).orElseThrow()
        auction.updateBid(auction.startPrice!! + 1000)
        auctionRepository.save(auction)

        val beforeScore = memberService.findByUsername("user1")!!.reputation!!.score

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/auctions/{auctionId}", auctionId)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deleteAuction"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."))

        val afterScore = memberService.findByUsername("user1")!!.reputation!!.score
        Assertions.assertThat(afterScore).isLessThan(beforeScore)
    }

    @Test
    @WithUserDetails("user2")
    @DisplayName("입찰 X -> 경매 취소 시 신용도 감소 X")
    @Throws(Exception::class)
    fun t14() {
        val auctionId = findAuctionIdBySellerAndBidCount("user2", 0)
        val beforeScore = memberService.findByUsername("user2")!!.reputation!!.score

        val resultActions = mvc
            .perform(
                MockMvcRequestBuilders.delete("/api/v1/auctions/{auctionId}", auctionId)
                    .with(SecurityMockMvcRequestPostProcessors.csrf())
            )
            .andDo(MockMvcResultHandlers.print())

        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(AuctionController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("deleteAuction"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("경매가 정상적으로 취소되었습니다."))

        val afterScore = memberService.findByUsername("user2")!!.reputation!!.score
        Assertions.assertThat(afterScore).isEqualTo(beforeScore)
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 실패 - 유효하지 않은 상태값")
    @Throws(Exception::class)
    fun t15() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auctions")
                .param("status", "INVALID")
                .param("page", "0")
                .param("size", "20")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 목록 조회 실패 - page 음수")
    @Throws(Exception::class)
    fun t16() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auctions")
                .param("page", "-1")
                .param("size", "20")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 실패 - 존재하지 않는 카테고리")
    @Throws(Exception::class)
    fun t17() {
        val request = mapOf(
            "name" to "잘못된 카테고리 상품",
            "description" to "테스트 설명",
            "startPrice" to "10000",
            "buyNowPrice" to "20000",
            "categoryId" to "99999",
            "durationHours" to "24"
        )

        mvc.perform(createAuctionRequest(request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-2"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 실패 - 필수값 누락(name)")
    @Throws(Exception::class)
    fun t18() {
        val request = mapOf(
            "description" to "테스트 설명",
            "startPrice" to "10000",
            "buyNowPrice" to "20000",
            "categoryId" to "1",
            "durationHours" to "24"
        )

        mvc.perform(createAuctionRequest(request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 등록 실패 - durationHours 최소값 위반")
    @Throws(Exception::class)
    fun t19() {
        val request = mapOf(
            "name" to "기간 오류 상품",
            "description" to "테스트 설명",
            "startPrice" to "10000",
            "buyNowPrice" to "20000",
            "categoryId" to "1",
            "durationHours" to "0"
        )

        mvc.perform(createAuctionRequest(request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @WithUserDetails("user2")
    @DisplayName("경매 수정 실패 - 권한 없음")
    @Throws(Exception::class)
    fun t20() {
        val auctionId = findAuctionIdBySeller("user1")
        val request = mapOf(
            "name" to "권한 없는 수정"
        )

        mvc.perform(patchAuctionRequest(auctionId, request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 수정 실패 - 입찰 발생 경매")
    @Throws(Exception::class)
    fun t21() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)
        val auction = auctionRepository.findById(auctionId).orElseThrow()
        auction.updateBid(auction.startPrice!! + 1000)
        auctionRepository.save(auction)

        val request = mapOf(
            "name" to "입찰 후 수정 시도"
        )

        mvc.perform(patchAuctionRequest(auctionId, request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 수정 실패 - 종료 시간이 현재보다 이전")
    @Throws(Exception::class)
    fun t22() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)
        val request = mapOf(
            "endAt" to "2000-01-01T00:00:00"
        )

        mvc.perform(patchAuctionRequest(auctionId, request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-4"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 수정 실패 - 즉시구매가가 시작가보다 낮음")
    @Throws(Exception::class)
    fun t23() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)
        val request = mapOf(
            "startPrice" to "50000",
            "buyNowPrice" to "10000"
        )

        mvc.perform(patchAuctionRequest(auctionId, request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-6"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("경매 수정 실패 - 존재하지 않는 경매")
    @Throws(Exception::class)
    fun t24() {
        val request = mapOf(
            "name" to "없는 경매 수정"
        )

        mvc.perform(patchAuctionRequest(99999, request))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("거래 취소 실패 - 낙찰 완료 전 상태")
    @Throws(Exception::class)
    fun t25() {
        val auctionId = findAuctionIdBySellerAndBidCount("user1", 0)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/cancel", auctionId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @WithUserDetails("user3")
    @DisplayName("거래 취소 실패 - 판매자/낙찰자가 아닌 사용자")
    @Throws(Exception::class)
    fun t26() {
        val auctionId = findAuctionIdBySeller("user1")
        val auction = auctionRepository.findById(auctionId).orElseThrow()
        val winner = memberService.findByUsername("user2")!!
        auction.completeWithWinner(winner.id)
        auctionRepository.save(auction)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/cancel", auctionId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-1"))
    }

    @Test
    @WithUserDetails("user2")
    @DisplayName("거래 취소 성공 - 낙찰자")
    @Throws(Exception::class)
    fun t27() {
        val auctionId = findAuctionIdBySeller("user1")
        val auction = auctionRepository.findById(auctionId).orElseThrow()
        val winner = memberService.findByUsername("user2")!!
        auction.completeWithWinner(winner.id)
        auctionRepository.save(auction)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/cancel", auctionId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))

        val cancelledAuction = auctionRepository.findById(auctionId).orElseThrow()
        Assertions.assertThat(cancelledAuction.status).isEqualTo(AuctionStatus.CANCELLED)
        Assertions.assertThat(cancelledAuction.cancelledBy).isEqualTo(winner.id)
        Assertions.assertThat(cancelledAuction.cancellerRole?.name).isEqualTo("BUYER")
    }

    @Test
    @WithUserDetails("user1")
    @DisplayName("거래 취소 실패 - 존재하지 않는 경매")
    @Throws(Exception::class)
    fun t28() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/cancel", 99999)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
    }
}
