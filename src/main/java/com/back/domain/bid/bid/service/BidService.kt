package com.back.domain.bid.bid.service

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.bid.bid.dto.request.BidCreateRequest
import com.back.domain.bid.bid.dto.response.BidListItemDto
import com.back.domain.bid.bid.dto.response.BidPageResponse
import com.back.domain.bid.bid.dto.response.BidResponse
import com.back.domain.bid.bid.entity.Bid
import com.back.domain.bid.bid.service.port.BidAuctionPort
import com.back.domain.bid.bid.service.port.BidMemberPort
import com.back.domain.bid.bid.service.port.BidPersistencePort
import com.back.domain.bid.bid.service.port.BidUseCase
import com.back.domain.member.member.entity.Member
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import com.back.global.util.PageUtils
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BidService(
    private val bidPersistencePort: BidPersistencePort,
    private val bidAuctionPort: BidAuctionPort,
    private val bidMemberPort: BidMemberPort
) : BidUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    @CacheEvict(value = ["auction"], key = "#auctionId")
    override fun createBid(auctionId: Int, request: BidCreateRequest, bidderId: Int): RsData<BidResponse> {
        val bidPrice = request.price
        log.debug("입찰 시작 - 경매 캐시 삭제: auctionId: {}, 입찰자 ID: {}, 입찰가: {}원", auctionId, bidderId, bidPrice)

        log.debug("비관적 락으로 경매 조회 - 경매 ID: {}", auctionId)
        val auction = bidAuctionPort.getAuctionWithLockOrThrow(auctionId)
        val bidder = bidMemberPort.getBidderOrThrow(bidderId)

        log.debug("입찰 검증 시작 - 경매 ID: {}, 현재가: {}원", auctionId, auction.currentHighestBid)
        validateBid(auction, bidder, bidPrice)

        val savedBid = bidPersistencePort.save(Bid(auction, bidder, bidPrice))
        auction.updateBid(bidPrice)

        val isBuyNow = if (auction.buyNowPrice != null && bidPrice == auction.buyNowPrice) {
            auction.closeAuction()
            log.info(
                "즉시구매 완료 - 경매 ID: {}, 구매자: {} ({}), 즉시구매가: {}원",
                auctionId,
                bidder.nickname,
                bidderId,
                bidPrice
            )
            true
        } else {
            log.info(
                "입찰 성공 - 경매 ID: {}, 입찰자: {} ({}), 입찰가: {}원, 입찰 횟수: {}",
                auctionId,
                bidder.nickname,
                bidderId,
                bidPrice,
                auction.bidCount
            )
            false
        }

        bidAuctionPort.saveAuction(auction)

        val response = BidResponse.from(savedBid, auction.currentHighestBid ?: bidPrice, auction.bidCount, isBuyNow)

        val message = if (isBuyNow) "즉시구매가 완료되었습니다." else "입찰에 성공했습니다."
        return RsData("200-1", message, response)
    }

    private fun validateBid(auction: Auction, bidder: Member, bidPrice: Int) {
        if (auction.isExpired()) {
            log.warn("입찰 실패 - 경매 종료: 경매 ID: {}, 종료 시간: {}", auction.id, auction.endAt)
            throw ServiceException("400-2", "종료된 경매입니다.")
        }

        if (!auction.isActive()) {
            log.warn("입찰 실패 - 경매 상태 오류: 경매 ID: {}, 상태: {}", auction.id, auction.status)
            throw ServiceException("400-1", "진행 중인 경매가 아닙니다.")
        }

        if (auction.isSeller(bidder.id)) {
            log.warn("입찰 실패 - 본인 경매 입찰 시도: 경매 ID: {}, 사용자 ID: {}", auction.id, bidder.id)
            throw ServiceException("403-1", "자신의 경매에는 입찰할 수 없습니다.")
        }

        val currentPrice = auction.currentHighestBid ?: auction.startPrice
        if (currentPrice == null) {
            throw ServiceException("500-1", "경매 가격 정보가 유효하지 않습니다.")
        }

        log.debug("입찰가 검증 - 현재가: {}원, 입찰가: {}원", currentPrice, bidPrice)

        if (bidPrice <= currentPrice) {
            log.warn("입찰 실패 - 입찰가 부족: 경매 ID: {}, 현재가: {}원, 입찰가: {}원", auction.id, currentPrice, bidPrice)
            throw ServiceException("400-3", String.format("입찰가는 현재 최고가(%,d원)보다 높아야 합니다.", currentPrice))
        }

        val maxAllowedPrice = (currentPrice * 1.5).toInt()
        if (bidPrice > maxAllowedPrice) {
            log.warn("입찰 실패 - 입찰가 초과: 경매 ID: {}, 입찰가: {}원, 최대 허용가: {}원", auction.id, bidPrice, maxAllowedPrice)
            throw ServiceException("400-4", String.format("입찰가는 현재가의 150%% (%,d원)를 초과할 수 없습니다.", maxAllowedPrice))
        }

        // 즉시구매가가 설정된 경매에서만 상한 검증을 적용한다.
        auction.buyNowPrice?.let { buyNowPrice ->
            if (bidPrice > buyNowPrice) {
                log.warn(
                    "입찰 실패 - 즉시구매가 초과: 경매 ID: {}, 입찰가: {}원, 즉시구매가: {}원",
                    auction.id,
                    bidPrice,
                    buyNowPrice
                )
                throw ServiceException("400-5", String.format("입찰가는 즉시구매가(%,d원)를 초과할 수 없습니다.", buyNowPrice))
            }
        }

        // 최고 입찰이 없는 첫 입찰 상황이면 null 이므로 안전 호출로 비교한다.
        val lastBid = bidPersistencePort.findTopByAuctionIdOrderByPriceDesc(auction.id)
        if (lastBid?.bidder?.id == bidder.id) {
            log.warn("입찰 실패 - 연속 입찰 시도: 경매 ID: {}, 사용자: {}", auction.id, bidder.nickname)
            throw ServiceException("400-6", "이미 최고가 입찰자입니다. 다른 입찰자가 입찰할 때까지 기다려주세요.")
        }

        log.debug("입찰 검증 완료 - 경매 ID: {}", auction.id)
    }

    override fun getBids(auctionId: Int, page: Int, size: Int): RsData<BidPageResponse> {
        log.debug("입찰 내역 조회 - 경매 ID: {}, 페이지: {}, 크기: {}", auctionId, page, size)

        if (!bidAuctionPort.existsAuction(auctionId)) {
            throw ServiceException("404-1", "존재하지 않는 경매입니다.")
        }

        val sort = Sort.by(Sort.Direction.DESC, "createdAt")
        val pageable = PageUtils.createPageable(page, size, sort)
        val bidPage = bidPersistencePort.findByAuctionId(auctionId, pageable)
        val dtoPage = bidPage.map(BidListItemDto::from)
        val response = BidPageResponse.from(dtoPage)

        log.debug("입찰 내역 조회 완료 - 경매 ID: {}, 전체 입찰 수: {}", auctionId, response.totalElements)
        return RsData("200-1", "입찰 내역 조회 성공", response)
    }
}
