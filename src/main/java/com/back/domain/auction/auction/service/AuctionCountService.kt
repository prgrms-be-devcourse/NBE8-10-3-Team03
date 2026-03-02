package com.back.domain.auction.auction.service

import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.service.port.AuctionPersistencePort
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AuctionCountService(
    private val auctionPersistencePort: AuctionPersistencePort
) {
    @Cacheable(value = ["auctionCount"], key = "#cacheKey", sync = true)
    fun getTotalCount(cacheKey: String, categoryId: Int?, status: AuctionStatus?): Long =
        when {
            categoryId != null && status != null -> auctionPersistencePort.countByCategoryIdAndStatus(categoryId, status)
            categoryId != null -> auctionPersistencePort.countByCategoryId(categoryId)
            status != null -> auctionPersistencePort.countByStatus(status)
            else -> auctionPersistencePort.countAll()
        }

    @CacheEvict(value = ["auctionCount"], allEntries = true)
    fun evictAll() = Unit
}
