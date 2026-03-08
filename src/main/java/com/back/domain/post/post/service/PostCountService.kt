package com.back.domain.post.post.service

import com.back.domain.auction.auction.entity.AuctionStatus
import com.back.domain.auction.auction.service.port.AuctionPersistencePort
import com.back.domain.post.post.entity.PostStatus
import com.back.domain.post.post.service.port.PostPort
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostCountService(
    private val postPort: PostPort
) {
    @Cacheable(value = ["postCount"], key = "#cacheKey", sync = true)
    fun getTotalCount(cacheKey: String, categoryId: Int?, status: PostStatus?): Long =
        when {
            categoryId != null && status != null -> postPort.countByCategoryIdAndStatus(categoryId, status)
            categoryId != null -> postPort.countByCategoryId(categoryId)
            status != null -> postPort.countByStatus(status)
            else -> postPort.countAll()
        }

    @CacheEvict(value = ["postCount"], allEntries = true)
    fun evictAll() = Unit
}

