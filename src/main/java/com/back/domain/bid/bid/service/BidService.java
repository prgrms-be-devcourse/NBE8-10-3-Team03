package com.back.domain.bid.bid.service;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.bid.bid.dto.request.BidCreateRequest;
import com.back.domain.bid.bid.dto.response.BidListItemDto;
import com.back.domain.bid.bid.dto.response.BidPageResponse;
import com.back.domain.bid.bid.dto.response.BidResponse;
import com.back.domain.bid.bid.entity.Bid;
import com.back.domain.bid.bid.repository.BidRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.global.exception.ServiceException;
import com.back.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public RsData<BidResponse> createBid(Integer auctionId, BidCreateRequest request, Integer bidderId) {
        // 1. 경매 조회
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 경매입니다."));

        // 2. 입찰자 조회
        Member bidder = memberRepository.findById(bidderId)
                .orElseThrow(() -> new ServiceException("404-2", "존재하지 않는 사용자입니다."));

        // 3. 입찰 검증
        validateBid(auction, bidder, request.getPrice());

        // 4. 입찰 생성 및 저장
        Bid bid = new Bid(auction, bidder, request.getPrice());
        Bid savedBid = bidRepository.save(bid);

        // 5. 경매 정보 업데이트
        auction.updateBid(request.getPrice());

        // 6. 즉시구매 처리
        boolean isBuyNow = false;
        if (auction.getBuyNowPrice() != null && request.getPrice().equals(auction.getBuyNowPrice())) {
            auction.closeAuction();
            isBuyNow = true;
        }

        auctionRepository.save(auction);

        // 7. 응답 생성
        BidResponse response = new BidResponse(
                savedBid,
                auction.getCurrentHighestBid(),
                auction.getBidCount(),
                isBuyNow
        );

        String message = isBuyNow ? "즉시구매가 완료되었습니다." : "입찰에 성공했습니다.";
        return new RsData<>("200-1", message, response);
    }

    private void validateBid(Auction auction, Member bidder, Integer bidPrice) {
        // 1. 경매 상태 확인
        if (!auction.isActive()) {
            throw new ServiceException("400-1", "진행 중인 경매가 아닙니다.");
        }

        // 2. 경매 종료 시간 확인
        if (auction.isExpired()) {
            throw new ServiceException("400-2", "종료된 경매입니다.");
        }

        // 3. 자신의 경매인지 확인
        if (auction.isSeller(bidder.getId())) {
            throw new ServiceException("403-1", "자신의 경매에는 입찰할 수 없습니다.");
        }

        // 4. 현재 최고가 확인
        Integer currentPrice = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid()
                : auction.getStartPrice();

        // 5. 입찰가가 현재 최고가보다 높은지 확인
        if (bidPrice <= currentPrice) {
            throw new ServiceException("400-3",
                    String.format("입찰가는 현재 최고가(%,d원)보다 높아야 합니다.", currentPrice));
        }

        // 6. 입찰가 50% 제한 확인
        Integer maxAllowedPrice = (int) (currentPrice * 1.5);
        if (bidPrice > maxAllowedPrice) {
            throw new ServiceException("400-4",
                    String.format("입찰가는 현재가의 150%% (%,d원)를 초과할 수 없습니다.", maxAllowedPrice));
        }

        // 7. 즉시구매가 초과 확인
        if (auction.getBuyNowPrice() != null && bidPrice > auction.getBuyNowPrice()) {
            throw new ServiceException("400-5",
                    String.format("입찰가는 즉시구매가(%,d원)를 초과할 수 없습니다.", auction.getBuyNowPrice()));
        }

        // 8. 연속 입찰 제한 확인
        Optional<Bid> lastBid = bidRepository.findTopByAuctionIdOrderByPriceDesc(auction.getId());
        if (lastBid.isPresent() && lastBid.get().getBidder().getId() == bidder.getId()) {
            throw new ServiceException("400-6", "이미 최고가 입찰자입니다. 다른 입찰자가 입찰할 때까지 기다려주세요.");
        }
    }

    public RsData<BidPageResponse> getBids(Integer auctionId, int page, int size) {
        // 1. 경매 존재 확인
        if (!auctionRepository.existsById(auctionId)) {
            throw new ServiceException("404-1", "존재하지 않는 경매입니다.");
        }

        // 2. 페이징 설정 (최신순)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 3. 입찰 내역 조회
        Page<Bid> bidPage = bidRepository.findByAuctionId(auctionId, pageable);

        // 4. DTO 변환
        Page<BidListItemDto> dtoPage = bidPage.map(BidListItemDto::new);

        BidPageResponse response = BidPageResponse.from(dtoPage);

        return new RsData<>("200-1", "입찰 내역 조회 성공", response);
    }
}

