package com.back.domain.auction.auction.service

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest
import com.back.domain.auction.auction.dto.request.AuctionUpdateRequest
import com.back.domain.auction.auction.dto.response.AuctionDeleteResponse
import com.back.domain.auction.auction.dto.response.AuctionDetailResponse
import com.back.domain.auction.auction.dto.response.AuctionIdResponse
import com.back.domain.auction.auction.dto.response.AuctionListItemDto
import com.back.domain.auction.auction.dto.response.AuctionPageResponse
import com.back.domain.auction.auction.dto.response.AuctionSliceResponse
import com.back.domain.auction.auction.dto.response.AuctionUpdateResponse
import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.service.port.AuctionImagePort
import com.back.domain.auction.auction.service.port.AuctionMemberPort
import com.back.domain.auction.auction.service.port.AuctionPersistencePort
import com.back.domain.auction.auction.service.port.AuctionUseCase
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import com.back.global.util.PageUtils
import com.back.domain.category.category.service.port.CategoryPort
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AuctionService(
    private val auctionPersistencePort: AuctionPersistencePort,
    private val categoryPort: CategoryPort,
    private val auctionMemberPort: AuctionMemberPort,
    private val auctionImagePort: AuctionImagePort
) : AuctionUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun createAuction(request: AuctionCreateRequest, sellerId: Int): RsData<AuctionIdResponse> {
        log.debug("경매 생성 시작 - 판매자 ID: {}, 물품명: {}, 시작가: {}원", sellerId, request.name, request.startPrice)

        auctionMemberPort.validateCanCreateAuction(sellerId)
        validateAuctionRequest(request)
        val categoryId = requireNotNull(request.categoryId) { "categoryId is required." }
        val name = requireNotNull(request.name) { "name is required." }
        val description = requireNotNull(request.description) { "description is required." }
        val startPrice = requireNotNull(request.startPrice) { "startPrice is required." }

        val seller = auctionMemberPort.getSellerOrThrow(sellerId)
        // 카테고리 조회는 CategoryPort를 통해 수행해 Repository/JPA 구현 의존을 차단한다.
        val category = categoryPort.getByIdOrThrow(categoryId)

        val startAt = LocalDateTime.now()
        val endAt = startAt.plusHours(request.durationHours.toLong())
        log.debug("경매 시간 설정 - 시작: {}, 종료: {}", startAt, endAt)

        val auction = Auction.builder()
            .seller(seller)
            .category(category)
            .name(name)
            .description(description)
            .startPrice(startPrice)
            .buyNowPrice(request.buyNowPrice)
            .startAt(startAt)
            .endAt(endAt)
            .build()

        val savedAuction = auctionPersistencePort.save(auction)

        request.images?.takeIf { it.isNotEmpty() }?.let { images ->
            log.debug("경매 이미지 저장 시작 - 경매 ID: {}, 이미지 수: {}", savedAuction.id, images.size)
            val savedImages = auctionImagePort.saveImages(savedAuction, images)

            savedAuction.thumbnailUrl =
                savedImages.firstOrNull()?.image?.url
        }

        log.info(
            "경매 생성 완료 - 경매 ID: {}, 판매자: {} ({}), 카테고리: {}, 시작가: {}원, 즉시구매가: {}원",
            savedAuction.id, seller.nickname, sellerId, category.name, request.startPrice, request.buyNowPrice
        )

        val responseData = AuctionIdResponse(savedAuction.id, "경매 물품이 등록되었습니다.")
        return RsData("201-1", "경매 물품이 등록되었습니다.", responseData)
    }

    private fun validateAuctionRequest(request: AuctionCreateRequest) {
        val startPrice = requireNotNull(request.startPrice) { "startPrice is required." }
        val buyNowPrice = request.buyNowPrice
        if (buyNowPrice != null && buyNowPrice < startPrice) {
            throw ServiceException("400-2", "즉시구매가는 시작가보다 높아야 합니다.")
        }
    }

    override fun getAuctions(
        page: Int,
        size: Int,
        sortBy: String?,
        categoryName: String?,
        status: String?
    ): RsData<AuctionPageResponse> {
        val pageable = PageUtils.createPageable(page, size, createSort(sortBy))
        val auctionStatus = parseAuctionStatus(status)

        val auctionPage = when {
            !categoryName.isNullOrBlank() && auctionStatus != null ->
                auctionPersistencePort.findByCategoryNameAndStatus(categoryName, auctionStatus, pageable)
            !categoryName.isNullOrBlank() ->
                auctionPersistencePort.findByCategoryName(categoryName, pageable)
            auctionStatus != null ->
                auctionPersistencePort.findByStatus(auctionStatus, pageable)
            else ->
                auctionPersistencePort.findAll(pageable)
        }

        val dtoPage = auctionPage.map { auction -> AuctionListItemDto(auction) }

        return RsData("200-1", "경매 목록 조회 성공", AuctionPageResponse.from(dtoPage))
    }

    override fun getAuctionsByUserId(
        userId: Int,
        page: Int,
        size: Int,
        sortBy: String?,
        status: String?
    ): RsData<AuctionSliceResponse?> {
        val pageable = PageUtils.createPageable(page, size, createSort(sortBy))
        val auctionStatus = parseAuctionStatus(status)

        val auctionSlice = if (auctionStatus != null) {
            auctionPersistencePort.findBySellerIdAndStatus(userId, auctionStatus, pageable)
        } else {
            auctionPersistencePort.findBySellerId(userId, pageable)
        }

        val dtoSlice = auctionSlice.map { auction -> AuctionListItemDto(auction) }

        return RsData("200-1", "경매 목록 조회 성공", AuctionSliceResponse.from(dtoSlice))
    }

    private fun parseAuctionStatus(status: String?): AuctionStatus? =
        status
            ?.takeIf { it.isNotBlank() }
            ?.let {
                runCatching { AuctionStatus.valueOf(it.uppercase()) }
                    .getOrElse {
                        throw ServiceException("400-1", "유효하지 않은 경매 상태입니다. (OPEN, CLOSED, COMPLETED, CANCELLED)")
                    }
            }

    private fun createSort(sortBy: String?): Sort {
        if (sortBy.isNullOrBlank()) return Sort.by(Sort.Direction.DESC, "createDate")

        val sortParams = sortBy.split(",")
        val property = sortParams[0]
        val direction = if (sortParams.size > 1 && sortParams[1].equals("asc", ignoreCase = true)) {
            Sort.Direction.ASC
        } else {
            Sort.Direction.DESC
        }
        return Sort.by(direction, property)
    }

    @Cacheable(value = ["auction"], key = "#auctionId")
    override fun getAuctionDetailData(auctionId: Int): AuctionDetailResponse {
        log.debug("[Cache Miss] DB에서 경매 조회 - auctionId: {}", auctionId)
        val auction = auctionPersistencePort.findWithDetailsByIdOrNull(auctionId)
            ?: throw ServiceException("404-1", "존재하지 않는 경매입니다.")
        return AuctionDetailResponse(auction)
    }

    @Transactional
    @CacheEvict(value = ["auction"], key = "#auctionId")
    override fun updateAuction(auctionId: Int, request: AuctionUpdateRequest, memberId: Int): RsData<AuctionUpdateResponse> {
        log.debug("경매 수정 시작 - 캐시 삭제: auctionId: {}", auctionId)

        val auction = auctionPersistencePort.findWithDetailsByIdOrNull(auctionId)
            ?: throw ServiceException("404-1", "존재하지 않는 경매입니다.")

        if (!auction.isSeller(memberId)) {
            throw ServiceException("403-1", "경매를 수정할 권한이 없습니다.")
        }

        if (auction.hasBids()) {
            throw ServiceException("400-1", "입찰이 발생한 경매는 수정할 수 없습니다.")
        }

        validateUpdateRequest(request, auction)
        auction.updateBeforeBid(
            request.name,
            request.description,
            request.startPrice,
            request.buyNowPrice,
            request.endAt
        )

        // 이미지를 명시적으로 전달한 경우에만 기존 이미지 교체 로직을 수행한다.
        if (!request.images.isNullOrEmpty()) {
            auctionImagePort.replaceImages(auction, request.keepImageUrls, request.images)
        }

        auctionPersistencePort.save(auction)
        val response = AuctionUpdateResponse(auction.id, "경매 물품이 수정되었습니다.")
        return RsData("200-1", "경매 물품이 수정되었습니다.", response)
    }

    private fun validateUpdateRequest(request: AuctionUpdateRequest, auction: Auction) {
        // 종료 시각 변경은 현재 이후 + 시작 이후라는 두 조건을 모두 만족해야 한다.
        request.endAt?.let { endAt ->
            if (endAt.isBefore(LocalDateTime.now())) {
                throw ServiceException("400-4", "종료 시간은 현재 시간 이후여야 합니다.")
            }
            if (endAt.isBefore(auction.startAt)) {
                throw ServiceException("400-5", "종료 시간은 시작 시간 이후여야 합니다.")
            }
        }

        val newStartPrice = request.startPrice ?: auction.startPrice
        val newBuyNowPrice = request.buyNowPrice ?: auction.buyNowPrice

        if (newBuyNowPrice != null && newStartPrice != null && newBuyNowPrice < newStartPrice) {
            throw ServiceException("400-6", "즉시구매가는 시작가보다 높아야 합니다.")
        }
    }

    @Transactional
    @CacheEvict(value = ["auction"], key = "#auctionId")
    override fun deleteAuction(auctionId: Int, memberId: Int): RsData<AuctionDeleteResponse> {
        log.debug("경매 삭제 시작 - 캐시 삭제: auctionId: {}, 요청자 ID: {}", auctionId, memberId)

        val auction = auctionPersistencePort.findByIdOrNull(auctionId)
            ?: throw ServiceException("404-1", "존재하지 않는 경매입니다.")

        if (!auction.isSeller(memberId)) {
            log.warn("경매 삭제 실패 - 권한 없음: 경매 ID: {}, 요청자 ID: {}", auctionId, memberId)
            throw ServiceException("403-1", "경매를 삭제할 권한이 없습니다.")
        }

        auctionMemberPort.applyCancelPenalty(auctionId, memberId)
        auctionPersistencePort.delete(auction)

        log.info("경매 삭제 완료 - 경매 ID: {}, 판매자 ID: {}, 입찰 수: {}", auctionId, memberId, auction.bidCount)
        val response = AuctionDeleteResponse("경매가 정상적으로 취소되었습니다.")
        return RsData("200-1", "경매가 정상적으로 취소되었습니다.", response)
    }

    @Transactional
    override fun cancelTrade(auctionId: Int, memberId: Int): RsData<Void> {
        log.debug("거래 취소 시작 - 경매 ID: {}, 요청자 ID: {}", auctionId, memberId)

        val auction = auctionPersistencePort.findByIdOrNull(auctionId)
            ?: throw ServiceException("404-1", "존재하지 않는 경매입니다.")

        val role = try {
            // 도메인 엔티티에서 상태/권한 규칙을 판정하고, 서비스 계층에서는 HTTP 에러 코드로 매핑한다.
            auction.determineCancellerRole(memberId)
        } catch (e: IllegalStateException) {
            log.warn("거래 취소 실패 - 상태 오류: 경매 ID: {}, 사유: {}", auctionId, e.message)
            throw ServiceException("400-1", e.message ?: "거래를 취소할 수 없습니다.")
        } catch (e: IllegalArgumentException) {
            log.warn("거래 취소 실패 - 권한 없음: 경매 ID: {}, 요청자 ID: {}", auctionId, memberId)
            throw ServiceException("403-1", e.message ?: "거래를 취소할 권한이 없습니다.")
        }

        auction.cancelTrade(memberId, role)
        auctionPersistencePort.save(auction)

        log.info("거래 취소 완료 - 경매 ID: {}, 취소자 역할: {}, 취소자 ID: {}", auctionId, role, memberId)
        return RsData("200-1", "거래가 취소되었습니다.", null)
    }
}
