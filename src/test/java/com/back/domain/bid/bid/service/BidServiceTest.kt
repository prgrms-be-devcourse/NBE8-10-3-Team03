package com.back.domain.bid.bid.service

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.bid.bid.dto.request.BidCreateRequest
import com.back.domain.bid.bid.entity.Bid
import com.back.domain.bid.bid.service.port.BidAuctionPort
import com.back.domain.bid.bid.service.port.BidMemberPort
import com.back.domain.bid.bid.service.port.BidPersistencePort
import com.back.domain.category.category.entity.Category
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.Role
import com.back.global.exception.ServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Answers
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class BidServiceTest {
    private val bidPersistencePort: BidPersistencePort = mock(BidPersistencePort::class.java) { invocation ->
        if (invocation.method.name == "save") {
            invocation.arguments[0]
        } else {
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }
    private val bidAuctionPort: BidAuctionPort = mock(BidAuctionPort::class.java)
    private val bidMemberPort: BidMemberPort = mock(BidMemberPort::class.java)

    private val bidService = BidService(bidPersistencePort, bidAuctionPort, bidMemberPort)

    @Test
    @DisplayName("입찰 성공 시 현재가/입찰수 갱신 후 성공 응답을 반환한다.")
    fun t1() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val auction = openAuction(id = 10, seller = seller, startPrice = 10000, buyNowPrice = 50000)

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(10)).thenReturn(auction)
        `when`(bidMemberPort.getBidderOrThrow(2)).thenReturn(bidder)
        `when`(bidPersistencePort.findTopByAuctionIdOrderByPriceDesc(10)).thenReturn(null)

        val result = bidService.createBid(10, BidCreateRequest(price = 12000), 2)

        assertThat(result.resultCode).isEqualTo("200-1")
        assertThat(result.data).isNotNull
        assertThat(result.data!!.isBuyNow).isFalse()
        assertThat(result.data!!.currentHighestBid).isEqualTo(12000)
        assertThat(result.data!!.bidCount).isEqualTo(1)
        verify(bidAuctionPort).saveAuction(auction)
    }

    @Test
    @DisplayName("즉시구매가와 동일한 입찰이면 즉시구매 처리된다.")
    fun t2() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val auction = openAuction(id = 11, seller = seller, startPrice = 10000, buyNowPrice = 15000)

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(11)).thenReturn(auction)
        `when`(bidMemberPort.getBidderOrThrow(2)).thenReturn(bidder)
        `when`(bidPersistencePort.findTopByAuctionIdOrderByPriceDesc(11)).thenReturn(null)

        val result = bidService.createBid(11, BidCreateRequest(price = 15000), 2)

        assertThat(result.data!!.isBuyNow).isTrue()
        assertThat(auction.isCompleted()).isTrue()
    }

    @Test
    @DisplayName("종료된 경매에는 입찰할 수 없다.")
    fun t3() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val expiredAuction = Auction.builder()
            .seller(seller)
            .category(Category("디지털"))
            .name("만료 경매")
            .description("desc")
            .startPrice(10000)
            .buyNowPrice(20000)
            .startAt(LocalDateTime.now().minusDays(2))
            .endAt(LocalDateTime.now().minusHours(1))
            .build().also { ReflectionTestUtils.setField(it, "id", 12) }

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(12)).thenReturn(expiredAuction)
        `when`(bidMemberPort.getBidderOrThrow(2)).thenReturn(bidder)

        assertThatThrownBy { bidService.createBid(12, BidCreateRequest(price = 11000), 2) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-2")
    }

    @Test
    @DisplayName("자신의 경매에는 입찰할 수 없다.")
    fun t4() {
        val seller = member(id = 1, username = "seller")
        val auction = openAuction(id = 13, seller = seller, startPrice = 10000, buyNowPrice = 20000)

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(13)).thenReturn(auction)
        `when`(bidMemberPort.getBidderOrThrow(1)).thenReturn(seller)

        assertThatThrownBy { bidService.createBid(13, BidCreateRequest(price = 11000), 1) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("403-1")
    }

    @Test
    @DisplayName("입찰가는 현재가보다 커야 한다.")
    fun t5() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val auction = openAuction(id = 14, seller = seller, startPrice = 10000, buyNowPrice = 30000)
        auction.updateBid(12000)

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(14)).thenReturn(auction)
        `when`(bidMemberPort.getBidderOrThrow(2)).thenReturn(bidder)

        assertThatThrownBy { bidService.createBid(14, BidCreateRequest(price = 12000), 2) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-3")
    }

    @Test
    @DisplayName("입찰가는 현재가의 150%를 초과할 수 없다.")
    fun t6() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val auction = openAuction(id = 15, seller = seller, startPrice = 10000, buyNowPrice = 50000)

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(15)).thenReturn(auction)
        `when`(bidMemberPort.getBidderOrThrow(2)).thenReturn(bidder)

        assertThatThrownBy { bidService.createBid(15, BidCreateRequest(price = 20000), 2) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-4")
    }

    @Test
    @DisplayName("즉시구매가보다 높은 입찰은 불가하다.")
    fun t7() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val auction = openAuction(id = 16, seller = seller, startPrice = 10000, buyNowPrice = 14000)
        auction.updateBid(10001)

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(16)).thenReturn(auction)
        `when`(bidMemberPort.getBidderOrThrow(2)).thenReturn(bidder)

        assertThatThrownBy { bidService.createBid(16, BidCreateRequest(price = 14500), 2) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-5")
    }

    @Test
    @DisplayName("최고가 입찰자의 연속 입찰은 불가하다.")
    fun t8() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val auction = openAuction(id = 17, seller = seller, startPrice = 10000, buyNowPrice = 50000)
        val topBid = Bid(auction, bidder, 12000)

        `when`(bidAuctionPort.getAuctionWithLockOrThrow(17)).thenReturn(auction)
        `when`(bidMemberPort.getBidderOrThrow(2)).thenReturn(bidder)
        `when`(bidPersistencePort.findTopByAuctionIdOrderByPriceDesc(17)).thenReturn(topBid)

        assertThatThrownBy { bidService.createBid(17, BidCreateRequest(price = 13000), 2) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-6")
    }

    @Test
    @DisplayName("존재하지 않는 경매 입찰 목록 조회는 예외다.")
    fun t9() {
        `when`(bidAuctionPort.existsAuction(999)).thenReturn(false)

        assertThatThrownBy { bidService.getBids(999, 0, 10) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("404-1")
    }

    @Test
    @DisplayName("입찰 목록 조회 성공 시 페이지 응답을 반환한다.")
    fun t10() {
        val seller = member(id = 1, username = "seller")
        val bidder = member(id = 2, username = "bidder")
        val auction = openAuction(id = 18, seller = seller, startPrice = 10000, buyNowPrice = 50000)
        val bid = Bid(auction, bidder, 12000).also { ReflectionTestUtils.setField(it, "id", 44) }
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))

        `when`(bidAuctionPort.existsAuction(18)).thenReturn(true)
        `when`(bidPersistencePort.findByAuctionId(18, pageable)).thenReturn(PageImpl(listOf(bid), pageable, 1))

        val result = bidService.getBids(18, 0, 10)

        assertThat(result.resultCode).isEqualTo("200-1")
        assertThat(result.data).isNotNull
        assertThat(result.data!!.content).hasSize(1)
        assertThat(result.data!!.content[0].price).isEqualTo(12000)
    }

    private fun member(id: Int, username: String): Member {
        val member = Member(username, "pw", "$username-nick", Role.USER, null)
        ReflectionTestUtils.setField(member, "id", id)
        return member
    }

    private fun openAuction(id: Int, seller: Member, startPrice: Int, buyNowPrice: Int?): Auction {
        return Auction.builder()
            .seller(seller)
            .category(Category("전자기기"))
            .name("테스트 경매")
            .description("설명")
            .startPrice(startPrice)
            .buyNowPrice(buyNowPrice)
            .startAt(LocalDateTime.now().minusHours(1))
            .endAt(LocalDateTime.now().plusHours(3))
            .build()
            .also { ReflectionTestUtils.setField(it, "id", id) }
    }
}
