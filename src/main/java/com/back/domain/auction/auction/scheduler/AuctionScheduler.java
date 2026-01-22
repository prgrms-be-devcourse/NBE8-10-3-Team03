package com.back.domain.auction.auction.scheduler;

import com.back.domain.auction.auction.entity.Auction;
import com.back.domain.auction.auction.entity.AuctionStatus;
import com.back.domain.auction.auction.repository.AuctionRepository;
import com.back.domain.bid.bid.entity.Bid;
import com.back.domain.bid.bid.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    /**
     * 매분마다 실행되어 종료된 경매를 자동으로 낙찰 처리
     */
    @Scheduled(cron = "0 * * * * *")  // 매분 0초에 실행
    @Transactional
    public void processExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // 종료 시간이 지난 OPEN 상태의 경매 조회
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndAtBefore(AuctionStatus.OPEN, now);

        if (expiredAuctions.isEmpty()) {
            return;
        }

        log.info("만료된 경매 {}건 처리 시작", expiredAuctions.size());

        for (Auction auction : expiredAuctions) {
            processExpiredAuction(auction);
        }

        log.info("만료된 경매 처리 완료");
    }

    private void processExpiredAuction(Auction auction) {
        // 최고 입찰 조회
        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdOrderByPriceDesc(auction.getId());

        if (highestBid.isPresent()) {
            // 입찰이 있으면 낙찰 처리
            Bid winningBid = highestBid.get();
            auction.completeWithWinner(winningBid.getBidder().getId());

            log.info("경매 ID {} 낙찰 완료 - 낙찰자: {}, 낙찰가: {}원",
                    auction.getId(),
                    winningBid.getBidder().getNickname(),
                    winningBid.getPrice());
        } else {
            // 입찰이 없으면 그냥 종료
            auction.closeWithoutBid();

            log.info("경매 ID {} 입찰 없이 종료", auction.getId());
        }

        auctionRepository.save(auction);
    }
}

