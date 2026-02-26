package com.back.domain.bid.bid.controller

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.AuthTokenService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.messaging.converter.JacksonJsonMessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.web.socket.WebSocketHttpHeaders
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import java.lang.reflect.Type
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BidWebSocketIntegrationTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var authTokenService: AuthTokenService

    @Autowired
    private lateinit var auctionRepository: AuctionRepository

    @Test
    fun `입찰 성공 시 구독 중인 클라이언트로 실시간 메시지가 전송된다`() {
        val auctionId = findOpenAuctionIdForBidder("user1")
        val bidPrice = nextValidBidPrice(auctionId)
        val destination = "/sub/v1/auctions/$auctionId"
        val messageQueue = LinkedBlockingQueue<Map<*, *>>()

        val user = memberRepository.findByUsername("user4").orElseThrow()
        val accessToken = authTokenService.genAccessToken(user)

        val stompClient = WebSocketStompClient(
            SockJsClient(listOf(WebSocketTransport(StandardWebSocketClient())))
        ).apply {
            // MappingJackson2MessageConverter 대체: 동일한 JSON 역직렬화를 제공하는 비권장 아님 컨버터 사용
            messageConverter = JacksonJsonMessageConverter()
        }

        val connectHeaders = StompHeaders().apply {
            add("Authorization", "Bearer $accessToken")
        }

        var session: StompSession? = null
        try {
            session = stompClient
                .connectAsync(
                    "ws://localhost:$port/ws",
                    WebSocketHttpHeaders(),
                    connectHeaders,
                    object : StompSessionHandlerAdapter() {}
                )
                .get(5, TimeUnit.SECONDS)

            session.subscribe(destination, object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders): Type = MutableMap::class.java

                override fun handleFrame(headers: StompHeaders, payload: Any?) {
                    @Suppress("UNCHECKED_CAST")
                    messageQueue.offer(payload as Map<*, *>)
                }
            })

            Thread.sleep(200)

            mockMvc.perform(
                MockMvcRequestBuilders.post("/api/v1/auctions/{auctionId}/bids", auctionId)
                    .header("Authorization", "Bearer user1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"price":$bidPrice}""")
            )
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.resultCode").value("200-1"))

            val wsMessage = messageQueue.poll(5, TimeUnit.SECONDS)
            assertThat(wsMessage).isNotNull
            assertThat(wsMessage!!["resultCode"]).isEqualTo("200-1")

            val data = wsMessage["data"] as Map<*, *>
            assertThat((data["auctionId"] as Number).toInt()).isEqualTo(auctionId)
            assertThat((data["price"] as Number).toInt()).isEqualTo(bidPrice)
            assertThat(data["buyNow"] as Boolean).isFalse()
        } finally {
            session?.disconnect()
            stompClient.stop()
        }
    }

    private fun findOpenAuctionIdForBidder(bidderUsername: String): Int {
        return auctionRepository.findAll()
            .firstOrNull { auction ->
                auction.isActive() &&
                    auction.bidCount == 0 &&
                    auction.currentHighestBid == null &&
                    auction.startPrice != null &&
                    auction.seller.username != bidderUsername
            }?.id
            ?: throw IllegalStateException("조건에 맞는 경매를 찾을 수 없습니다.")
    }

    private fun nextValidBidPrice(auctionId: Int): Int {
        val auction = auctionRepository.findById(auctionId).orElseThrow()
        return (auction.startPrice ?: 10_000) + 1000
    }
}
