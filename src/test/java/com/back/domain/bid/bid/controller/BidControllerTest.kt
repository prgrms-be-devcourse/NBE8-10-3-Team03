package com.back.domain.bid.bid.controller

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.bid.bid.dto.response.BidResponse
import com.back.global.rsData.RsData
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.invocation.Invocation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BidControllerTest {
    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var auctionRepository: AuctionRepository

    @MockitoSpyBean
    private lateinit var messagingTemplate: SimpMessagingTemplate

    @BeforeEach
    fun setUp() {
        Mockito.reset(messagingTemplate)
    }

    @Test
    @DisplayName("1. 입찰 생성 성공")
    @Throws(Exception::class)
    fun t1() {
        val auctionId = 1 // seller: user2, startPrice: 11,000

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":12000}
                                
                                """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(BidController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("createBid"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.price").value(12000))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.auctionId").value(auctionId))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.buyNow").value(false))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isEqualTo(1)
        Assertions.assertThat(
            payloadsForAuction(auctionId).stream()
                .anyMatch { payload: Any? -> payload is RsData<*> && "200-1" == payload.resultCode }
        ).isTrue()
    }

    @Test
    @DisplayName("2. 입찰 생성 실패 - 입찰가 부족")
    @Throws(Exception::class)
    fun t2() {
        val auctionId = 2 // startPrice: 12,000

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":12000}
                                
                                """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-3"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("3. 입찰 생성 - 즉시구매가로 입찰 시 경매 즉시 종료")
    @Throws(Exception::class)
    fun t3() {
        val auctionId = 1 // startPrice: 11,000, buyNowPrice: 22,000

        // 현재가를 먼저 올려야 buyNow 입찰(22,000)이 150% 제한에 걸리지 않는다.
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":16000}
                                
                                """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user3"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":22000}
                                
                                """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.handler().handlerType(BidController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("createBid"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.price").value(22000))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.buyNow").value(true))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isEqualTo(2)

        Assertions.assertThat(
            payloadsForAuction(auctionId).stream()
                .filter { payload: Any? -> payload is RsData<*> }
                .map { payload: Any? -> (payload as RsData<*>).data }
                .anyMatch { data: Any? -> data is BidResponse && data.isBuyNow }
        ).isTrue()
    }

    @Test
    @DisplayName("4. 입찰 생성 실패 - 자신의 경매에 입찰")
    @Throws(Exception::class)
    fun t4() {
        val auctionId = 5 // seller: user1

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":16000}
                                
                                """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("403-1"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("5. 입찰 생성 실패 - 존재하지 않는 경매")
    @Throws(Exception::class)
    fun t5() {
        val invalidAuctionId = 99999

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", invalidAuctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":50000}
                                
                                """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))

        Assertions.assertThat(uniqueBroadcastCount(invalidAuctionId)).isZero()
    }

    @Test
    @DisplayName("6. 입찰 생성 실패 - 연속 입찰 시도")
    @Throws(Exception::class)
    fun t6() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":13000}
                                
                                """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":14000}
                                
                                """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-6"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isEqualTo(1)
    }

    @Test
    @DisplayName("7. 입찰 생성 실패 - 최고 입찰가 기준 50% 초과")
    @Throws(Exception::class)
    fun t7() {
        val auctionId = 3 // startPrice: 13,000, maxAllowed: 19,500

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":20000}
                                
                                """.trimIndent()
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-4"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("8. 특정 경매의 입찰 목록 조회 성공")
    @Throws(Exception::class)
    fun t8() {
        val auctionId = 4

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {"price":15000}
                                
                                """.trimIndent()
                )
        )
            .andExpect(MockMvcResultMatchers.status().isOk())

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", auctionId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.handler().handlerType(BidController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getBids"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].price").value(15000))
    }

    @Test
    @DisplayName("9. 입찰 목록 조회 - 입찰 없는 경매")
    @Throws(Exception::class)
    fun t9() {
        val auctionId = 6 // 존재하는 경매, 기본 데이터에서 입찰 없음

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", auctionId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content").isArray())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalElements").value(0))
    }

    @Test
    @DisplayName("10. 입찰 생성 실패 - 입찰가 누락")
    @Throws(Exception::class)
    fun t10() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("11. 입찰 생성 실패 - 즉시구매가 초과")
    @Throws(Exception::class)
    fun t11() {
        val auctionId = 1 // startPrice: 11,000, buyNowPrice: 22,000

        // 현재가를 16,000으로 높여 400-4(150% 초과)보다 400-5(즉시구매가 초과)가 먼저 검증되도록 만든다.
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":16000}""")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user3"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":23000}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-5"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isEqualTo(1)
    }

    @Test
    @DisplayName("12. 입찰 생성 실패 - 미인증 사용자")
    @Throws(Exception::class)
    fun t12() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":13000}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-1"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("13. 입찰 목록 조회 실패 - 존재하지 않는 경매")
    @Throws(Exception::class)
    fun t13() {
        val invalidAuctionId = 99999

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", invalidAuctionId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("404-1"))
    }

    @Test
    @DisplayName("14. 입찰 생성 실패 - 잘못된 JSON 본문")
    @Throws(Exception::class)
    fun t14() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("15. 입찰 생성 실패 - 입찰가 최소값 위반")
    @Throws(Exception::class)
    fun t15() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":0}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("16. 입찰 생성 실패 - 유효하지 않은 Authorization 헤더")
    @Throws(Exception::class)
    fun t16() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", "Bearer not-exists-user")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":13000}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("401-3"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("17. 입찰 생성 실패 - 진행 중이 아닌 경매 상태")
    @Throws(Exception::class)
    fun t17() {
        val auctionId = 2
        val auction = auctionRepository.findById(auctionId).orElseThrow()
        auction.closeAuction() // COMPLETED 상태로 전환

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":13000}""")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))

        Assertions.assertThat(uniqueBroadcastCount(auctionId)).isZero()
    }

    @Test
    @DisplayName("18. 입찰 목록 조회 실패 - page 음수")
    @Throws(Exception::class)
    fun t18() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", auctionId)
                .param("page", "-1")
                .param("size", "20")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("19. 입찰 목록 조회 실패 - size 0")
    @Throws(Exception::class)
    fun t19() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", auctionId)
                .param("page", "0")
                .param("size", "0")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("20. 입찰 목록 조회 실패 - size 최대값 초과")
    @Throws(Exception::class)
    fun t20() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", auctionId)
                .param("page", "0")
                .param("size", "101")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("400-1"))
    }

    @Test
    @DisplayName("21. 입찰 목록 조회 성공 - 페이징/정렬 검증")
    @Throws(Exception::class)
    fun t21() {
        val auctionId = 2

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":13000}""")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user2"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":15000}""")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                .header("Authorization", bearer("user4"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"price":18000}""")
        )
            .andExpect(MockMvcResultMatchers.status().isOk())

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", auctionId)
                .param("page", "0")
                .param("size", "2")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.size").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalElements").value(3))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.totalPages").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content.length()").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[0].price").value(18000))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.content[1].price").value(15000))
    }

    @Test
    @DisplayName("22. 입찰 목록 조회 성공 - 기본 페이징 파라미터")
    @Throws(Exception::class)
    fun t22() {
        val auctionId = 2

        mvc.perform(MockMvcRequestBuilders.get("/api/v1/auctions/{auctionId}/bids", auctionId))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.page").value(0))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.size").value(20))
    }

    private fun bearer(apiKey: String): String {
        return "Bearer " + apiKey
    }

    private fun uniqueBroadcastCount(auctionId: Int): Long {
        val uniquePayloads = Collections.newSetFromMap<Any?>(IdentityHashMap<Any?, Boolean?>())
        uniquePayloads.addAll(payloadsForAuction(auctionId))
        return uniquePayloads.size.toLong()
    }

    private fun payloadsForAuction(auctionId: Int): MutableList<Any?> {
        val destination = "/sub/v1/auctions/" + auctionId
        return Mockito.mockingDetails(messagingTemplate).getInvocations().stream()
            .filter { invocation: Invocation? -> "convertAndSend" == invocation!!.getMethod().getName() }
            .filter { invocation: Invocation? -> invocation!!.getArguments().size >= 2 }
            .filter { invocation: Invocation? -> destination == invocation!!.getArguments()[0] }
            .map<Any?> { invocation: Invocation? -> invocation!!.getArguments()[1] }
            .toList()
    }
}
