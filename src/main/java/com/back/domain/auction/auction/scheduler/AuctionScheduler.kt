package com.back.domain.auction.auction.scheduler

import com.back.domain.auction.auction.entity.Auction
import com.back.domain.auction.auction.service.port.AuctionPersistencePort
import com.back.domain.auction.auction.service.port.BidReadPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class AuctionScheduler(
    private val auctionPersistencePort: AuctionPersistencePort,
    private val bidReadPort: BidReadPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    fun processExpiredAuctions() {
        val now = LocalDateTime.now()
        val expiredAuctions = auctionPersistencePort.findExpiredOpenAuctions(now)
        if (expiredAuctions.isEmpty()) return

        log.info("만료된 경매 {}건 처리 시작", expiredAuctions.size)
        expiredAuctions.forEach { processExpiredAuction(it) }
        log.info("만료된 경매 처리 완료")
    }

    private fun processExpiredAuction(auction: Auction) {
        // 최고 입찰이 있으면 낙찰 완료, 없으면 유찰 종료로 상태를 전이한다.
        val winningBid = bidReadPort.findHighestBidByAuctionId(auction.id)
        if (winningBid != null) {
            auction.completeWithWinner(winningBid.bidderId)

            log.info(
                "경매 ID {} 낙찰 완료 - 낙찰자: {}, 낙찰가: {}원",
                auction.id,
                winningBid.bidderNickname,
                winningBid.price
            )
        } else {
            auction.closeWithoutBid()
            log.info("경매 ID {} 입찰 없이 종료", auction.id)
        }
        auctionPersistencePort.save(auction)
    }
}
