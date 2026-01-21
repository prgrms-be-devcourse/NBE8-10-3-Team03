package com.back.domain.auction.auction.service;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.dto.request.AuctionUpdateRequest;
import com.back.domain.auction.auction.dto.response.AuctionDeleteResponse;
import com.back.domain.auction.auction.dto.response.AuctionDetailResponse;
import com.back.domain.auction.auction.dto.response.AuctionIdResponse;
import com.back.domain.auction.auction.dto.response.AuctionListItemDto;
import com.back.domain.auction.auction.dto.response.AuctionPageResponse;
import com.back.domain.auction.auction.dto.response.AuctionUpdateResponse;
import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.entity.AuctionImage;
import com.back.domain.auction.auction.entity.AuctionStatus;
import com.back.domain.auction.auction.repository.AuctionImageRepository;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.service.MemberService;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final FileStorageService fileStorageService;
    private final MemberService memberService;

    @Transactional
    public RsData<AuctionIdResponse> createAuction(AuctionCreateRequest request, Integer sellerId) {
        // 1. 필수 값 검증
        validateAuctionRequest(request);

        // 2. 판매자 조회 (현재는 임시로 sellerId 사용, 추후 JWT에서 추출)
        Member seller = memberRepository.findById(sellerId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 사용자입니다."));

        // 3. 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ServiceException("404-2", "존재하지 않는 카테고리입니다."));

        // 4. 경매 시작/종료 시간 설정
        LocalDateTime startAt = LocalDateTime.now();
        LocalDateTime endAt = startAt.plusHours(request.getDurationHours());

        // 5. 경매 엔티티 생성
        Auction auction = Auction.builder()
                .seller(seller)
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .startPrice(request.getStartPrice())
                .buyNowPrice(request.getBuyNowPrice())
                .startAt(startAt)
                .endAt(endAt)
                .build();

        // 6. 경매 저장
        Auction savedAuction = auctionRepository.save(auction);

        // 7. 이미지 처리 (선택사항)
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            saveAuctionImages(request.getImages(), savedAuction);
        }

        AuctionIdResponse responseData = new AuctionIdResponse(
                savedAuction.getId(),
                "경매 물품이 등록되었습니다."
        );

        return new RsData<>("201-1", "경매 물품이 등록되었습니다.", responseData);
    }

    private void validateAuctionRequest(AuctionCreateRequest request) {
        // 가격 음수 검증은 @Min 어노테이션으로 처리됨

        // 즉시구매가가 시작가보다 낮은 경우 검증
        if (request.getBuyNowPrice() != null && request.getBuyNowPrice() < request.getStartPrice()) {
            throw new ServiceException("400-2", "즉시구매가는 시작가보다 높아야 합니다.");
        }
    }

    private void saveAuctionImages(List<MultipartFile> imageFiles, Auction auction) {
        for (MultipartFile file : imageFiles) {
            if (file.isEmpty()) {
                continue;
            }

            try {
                // 파일 저장
                String imageUrl = fileStorageService.storeFile(file);

                // Image 엔티티 생성 및 저장
                Image image = new Image(imageUrl);
                Image savedImage = imageRepository.save(image);

                // AuctionImage 조인 테이블 생성 및 저장
                AuctionImage auctionImage = new AuctionImage(auction, savedImage);
                auction.addAuctionImage(auctionImage);
            } catch (Exception e) {
                throw new ServiceException("500-1", "이미지 저장에 실패했습니다: " + e.getMessage());
            }
        }
    }

    public RsData<AuctionPageResponse> getAuctions(
            int page,
            int size,
            String sortBy,
            String categoryName,
            String status
    ) {
        // 파라미터 검증
        if (page < 0) {
            throw new ServiceException("400-1", "페이지 번호는 0 이상이어야 합니다.");
        }
        if (size <= 0) {
            throw new ServiceException("400-1", "페이지 크기는 1 이상이어야 합니다.");
        }

        // 정렬 설정
        Sort sort = createSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        // 상태 변환
        AuctionStatus auctionStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                auctionStatus = AuctionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ServiceException("400-1", "유효하지 않은 경매 상태입니다. (OPEN, CLOSED)");
            }
        }

        // 조건에 맞는 경매 조회
        Page<Auction> auctionPage;

        if (categoryName != null && !categoryName.isBlank() && auctionStatus != null) {
            auctionPage = auctionRepository.findByCategoryNameAndStatus(categoryName, auctionStatus, pageable);
        } else if (categoryName != null && !categoryName.isBlank()) {
            auctionPage = auctionRepository.findByCategoryName(categoryName, pageable);
        } else if (auctionStatus != null) {
            auctionPage = auctionRepository.findByStatus(auctionStatus, pageable);
        } else {
            auctionPage = auctionRepository.findAll(pageable);
        }

        // DTO 변환
        Page<AuctionListItemDto> dtoPage = auctionPage.map(auction -> {
            String thumbnailUrl = getThumbnailUrl(auction);
            return new AuctionListItemDto(auction, thumbnailUrl);
        });

        AuctionPageResponse response = AuctionPageResponse.from(dtoPage);

        return new RsData<>("200-1", "경매 목록 조회 성공", response);
    }

    private Sort createSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            // 기본 정렬: 최신순
            return Sort.by(Sort.Direction.DESC, "createDate");
        }

        String[] sortParams = sortBy.split(",");
        String property = sortParams[0];
        Sort.Direction direction = Sort.Direction.DESC;

        if (sortParams.length > 1) {
            direction = sortParams[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
        }

        return Sort.by(direction, property);
    }

    private String getThumbnailUrl(Auction auction) {
        if (auction.getAuctionImages() == null || auction.getAuctionImages().isEmpty()) {
            return null;
        }

        // 첫 번째 이미지를 썸네일로 사용
        return auction.getAuctionImages().get(0).getImage().getUrl();
    }

    public RsData<AuctionDetailResponse> getAuctionDetail(Integer auctionId) {
        Auction auction = auctionRepository.findWithDetailsById(auctionId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 경매입니다."));

        AuctionDetailResponse response = new AuctionDetailResponse(auction);

        return new RsData<>("200-1", "경매 상세 조회 성공", response);
    }

    @Transactional
    public RsData<AuctionUpdateResponse> updateAuction(Integer auctionId, AuctionUpdateRequest request, Integer memberId) {
        // 1. 경매 조회
        Auction auction = auctionRepository.findWithDetailsById(auctionId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 경매입니다."));

        // 2. 판매자 권한 확인
        if (!auction.isSeller(memberId)) {
            throw new ServiceException("403-1", "경매를 수정할 권한이 없습니다.");
        }

        // 3. 입찰 여부에 따른 수정 제한
        if (auction.hasBids()) {
            // 입찰이 있는 경우: 모든 수정 불가
            throw new ServiceException("400-1", "입찰이 발생한 경매는 수정할 수 없습니다.");
        }

        // 입찰이 없는 경우: 모든 필드 수정 가능
        validateUpdateRequest(request, auction);
        auction.updateBeforeBid(
                request.getName(),
                request.getDescription(),
                request.getStartPrice(),
                request.getBuyNowPrice(),
                request.getEndAt()
        );

        // 4. 이미지 처리
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            updateAuctionImages(request, auction);
        }

        // 5. 저장
        auctionRepository.save(auction);

        AuctionUpdateResponse response = new AuctionUpdateResponse(
                auction.getId(),
                "경매 물품이 수정되었습니다."
        );

        return new RsData<>("200-1", "경매 물품이 수정되었습니다.", response);
    }

    private void validateUpdateRequest(AuctionUpdateRequest request, Auction auction) {
        // 종료 시간 검증
        if (request.getEndAt() != null) {
            if (request.getEndAt().isBefore(LocalDateTime.now())) {
                throw new ServiceException("400-4", "종료 시간은 현재 시간 이후여야 합니다.");
            }
            if (request.getEndAt().isBefore(auction.getStartAt())) {
                throw new ServiceException("400-5", "종료 시간은 시작 시간 이후여야 합니다.");
            }
        }

        // 가격 검증
        Integer newStartPrice = request.getStartPrice() != null ? request.getStartPrice() : auction.getStartPrice();
        Integer newBuyNowPrice = request.getBuyNowPrice() != null ? request.getBuyNowPrice() : auction.getBuyNowPrice();

        if (newBuyNowPrice != null && newBuyNowPrice < newStartPrice) {
            throw new ServiceException("400-6", "즉시구매가는 시작가보다 높아야 합니다.");
        }
    }

    private void updateAuctionImages(AuctionUpdateRequest request, Auction auction) {
        // 기존 이미지 중 유지할 이미지 확인
        List<String> keepUrls = request.getKeepImageUrls();

        if (keepUrls == null || keepUrls.isEmpty()) {
            // 모든 이미지 삭제
            auction.clearAuctionImages();
        } else {
            // 유지하지 않을 이미지만 삭제
            auction.getAuctionImages().removeIf(auctionImage ->
                !keepUrls.contains(auctionImage.getImage().getUrl())
            );
        }

        // 새 이미지 추가
        for (MultipartFile file : request.getImages()) {
            if (file.isEmpty()) {
                continue;
            }

            try {
                String imageUrl = fileStorageService.storeFile(file);
                Image image = new Image(imageUrl);
                Image savedImage = imageRepository.save(image);
                AuctionImage auctionImage = new AuctionImage(auction, savedImage);
                auction.addAuctionImage(auctionImage);
            } catch (Exception e) {
                throw new ServiceException("500-1", "이미지 저장에 실패했습니다: " + e.getMessage());
            }
        }
    }

    @Transactional
    public RsData<AuctionDeleteResponse> deleteAuction(Integer auctionId, Integer memberId) {
        // 1. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 경매입니다."));

        // 2. 판매자 권한 확인
        if (!auction.isSeller(memberId)) {
            throw new ServiceException("403-1", "경매를 삭제할 권한이 없습니다.");
        }

        // 3. 입찰 O -> 판매자 신용도 감소
        memberService.decreaseByCancel(auctionId, memberId);

        // 4. 경매 삭제
        auctionRepository.delete(auction);

        AuctionDeleteResponse response = new AuctionDeleteResponse("경매가 정상적으로 취소되었습니다.");

        return new RsData<>("200-1", "경매가 정상적으로 취소되었습니다.", response);
    }
}
