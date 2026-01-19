package com.back.domain.auction.auction.service;

import com.back.domain.auction.auction.dto.request.AuctionCreateRequest;
import com.back.domain.auction.auction.dto.response.AuctionIdResponse;
import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.entity.AuctionImage;
import com.back.domain.auction.auction.repository.AuctionImageRepository;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.category.category.entity.Category;
import com.back.domain.category.category.repository.CategoryRepository;
import com.back.domain.image.image.entity.Image;
import com.back.domain.image.image.repository.ImageRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
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
}

