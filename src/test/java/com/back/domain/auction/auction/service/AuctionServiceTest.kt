package com.back.domain.auction.auction.service

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest
import com.back.domain.auction.auction.dto.request.AuctionUpdateRequest
import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.service.port.AuctionImagePort
import com.back.domain.auction.auction.service.port.AuctionMemberPort
import com.back.domain.auction.auction.service.port.AuctionPersistencePort
import com.back.domain.category.category.entity.Category
import com.back.domain.category.category.service.port.CategoryPort
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
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDateTime

class AuctionServiceTest {
    private val auctionPersistencePort: AuctionPersistencePort = mock(AuctionPersistencePort::class.java) { invocation ->
        if (invocation.method.name == "save") {
            invocation.arguments[0]
        } else {
            Answers.RETURNS_DEFAULTS.answer(invocation)
        }
    }
    private val categoryPort: CategoryPort = mock(CategoryPort::class.java)
    private val auctionMemberPort: AuctionMemberPort = mock(AuctionMemberPort::class.java)
    private val auctionImagePort: AuctionImagePort = mock(AuctionImagePort::class.java)

    private val auctionService = AuctionService(
        auctionPersistencePort,
        categoryPort,
        auctionMemberPort,
        auctionImagePort
    )

    @Test
    @DisplayName("경매 생성 성공 시 저장된 경매 ID를 반환한다.")
    fun t1() {
        val seller = member(1, "seller")
        val category = Category("전자기기")
        val req = AuctionCreateRequest().apply {
            name = "맥북"
            description = "거의 새상품"
            startPrice = 100000
            buyNowPrice = 200000
            categoryId = 10
            durationHours = 24
            images = listOf(MockMultipartFile("img", "a.jpg", "image/jpeg", "x".toByteArray()))
        }

        `when`(auctionMemberPort.getSellerOrThrow(1)).thenReturn(seller)
        `when`(categoryPort.getByIdOrThrow(10)).thenReturn(category)

        val result = auctionService.createAuction(req, 1)

        assertThat(result.resultCode).isEqualTo("201-1")
        assertThat(result.data!!.auctionId).isZero()
        verify(auctionMemberPort).validateCanCreateAuction(1)
    }

    @Test
    @DisplayName("경매 생성 시 즉시구매가가 시작가보다 낮으면 예외다.")
    fun t2() {
        val req = AuctionCreateRequest().apply {
            name = "테스트"
            description = "설명"
            startPrice = 20000
            buyNowPrice = 15000
            categoryId = 1
            durationHours = 24
        }

        assertThatThrownBy { auctionService.createAuction(req, 1) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-2")
    }

    @Test
    @DisplayName("상태 파라미터가 유효하지 않으면 목록 조회에서 예외가 발생한다.")
    fun t3() {
        assertThatThrownBy { auctionService.getAuctions(0, 20, null, null, "INVALID") }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-1")
    }

    @Test
    @DisplayName("유저별 경매 목록은 상태 필터 없이 조회 가능하다.")
    fun t4() {
        val seller = member(1, "seller")
        val auction = openAuction(20, seller)
        val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"))
        `when`(auctionPersistencePort.findBySellerId(1, pageable)).thenReturn(PageImpl(listOf(auction), pageable, 1))

        val result = auctionService.getAuctionsByUserId(1, 0, 10, "id,desc", null)

        assertThat(result.resultCode).isEqualTo("200-1")
        assertThat(result.data!!.content).hasSize(1)
    }

    @Test
    @DisplayName("경매 수정 시 대상이 없으면 404 예외다.")
    fun t5() {
        `when`(auctionPersistencePort.findWithDetailsByIdOrNull(999)).thenReturn(null)

        assertThatThrownBy { auctionService.updateAuction(999, AuctionUpdateRequest(), 1) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("404-1")
    }

    @Test
    @DisplayName("경매 수정은 판매자만 가능하다.")
    fun t6() {
        val seller = member(1, "seller")
        val auction = openAuction(21, seller)
        `when`(auctionPersistencePort.findWithDetailsByIdOrNull(21)).thenReturn(auction)

        assertThatThrownBy { auctionService.updateAuction(21, AuctionUpdateRequest(), 2) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("403-1")
    }

    @Test
    @DisplayName("입찰이 있는 경매는 수정할 수 없다.")
    fun t7() {
        val seller = member(1, "seller")
        val auction = openAuction(22, seller).also { it.updateBid(11000) }
        `when`(auctionPersistencePort.findWithDetailsByIdOrNull(22)).thenReturn(auction)

        assertThatThrownBy { auctionService.updateAuction(22, AuctionUpdateRequest(), 1) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-1")
    }

    @Test
    @DisplayName("종료 시각은 현재 이후여야 한다.")
    fun t8() {
        val seller = member(1, "seller")
        val auction = openAuction(23, seller)
        val req = AuctionUpdateRequest().apply { endAt = LocalDateTime.now().minusMinutes(1) }
        `when`(auctionPersistencePort.findWithDetailsByIdOrNull(23)).thenReturn(auction)

        assertThatThrownBy { auctionService.updateAuction(23, req, 1) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-4")
    }

    @Test
    @DisplayName("즉시구매가는 시작가보다 높아야 한다.")
    fun t9() {
        val seller = member(1, "seller")
        val auction = openAuction(24, seller)
        val req = AuctionUpdateRequest().apply {
            startPrice = 30000
            buyNowPrice = 20000
        }
        `when`(auctionPersistencePort.findWithDetailsByIdOrNull(24)).thenReturn(auction)

        assertThatThrownBy { auctionService.updateAuction(24, req, 1) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-6")
    }

    @Test
    @DisplayName("경매 삭제는 판매자만 가능하다.")
    fun t10() {
        val seller = member(1, "seller")
        val auction = openAuction(25, seller)
        `when`(auctionPersistencePort.findByIdOrNull(25)).thenReturn(auction)

        assertThatThrownBy { auctionService.deleteAuction(25, 9) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("403-1")
    }

    @Test
    @DisplayName("거래 취소는 낙찰 완료 상태가 아니면 불가하다.")
    fun t11() {
        val seller = member(1, "seller")
        val auction = openAuction(26, seller) // OPEN
        `when`(auctionPersistencePort.findByIdOrNull(26)).thenReturn(auction)

        assertThatThrownBy { auctionService.cancelTrade(26, 1) }
            .isInstanceOf(ServiceException::class.java)
            .extracting("resultCode")
            .isEqualTo("400-1")
    }

    @Test
    @DisplayName("거래 취소 성공 시 저장되고 200을 반환한다.")
    fun t12() {
        val seller = member(1, "seller")
        val buyer = member(2, "buyer")
        val auction = openAuction(27, seller).apply {
            completeWithWinner(buyer.id)
        }
        `when`(auctionPersistencePort.findByIdOrNull(27)).thenReturn(auction)

        val result = auctionService.cancelTrade(27, 2)

        assertThat(result.resultCode).isEqualTo("200-1")
        assertThat(auction.status).isEqualTo(AuctionStatus.CANCELLED)
        verify(auctionPersistencePort).save(auction)
        verify(auctionMemberPort, never()).applyCancelPenalty(27, 2)
    }

    private fun member(id: Int, username: String): Member {
        val member = Member(username, "pw", "$username-nick", Role.USER, null)
        ReflectionTestUtils.setField(member, "id", id)
        return member
    }

    private fun openAuction(id: Int, seller: Member): Auction {
        return Auction.builder()
            .seller(seller)
            .category(Category("전자기기"))
            .name("테스트 경매")
            .description("설명")
            .startPrice(10000)
            .buyNowPrice(30000)
            .startAt(LocalDateTime.now().minusHours(1))
            .endAt(LocalDateTime.now().plusHours(3))
            .build()
            .also { ReflectionTestUtils.setField(it, "id", id) }
    }
}
