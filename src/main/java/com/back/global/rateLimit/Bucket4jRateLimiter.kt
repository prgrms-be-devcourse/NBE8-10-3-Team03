package com.back.global.rateLimit

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.function.Function

@Component
class Bucket4jRateLimiter {
    private val cache: Cache<String, Bucket> = Caffeine.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(100000)
        .build<String, Bucket>()


    fun tryConsume(key: String?, policy: RateLimitPolicy): Boolean =
        cache.get(key) {
            Bucket.builder()
                .addLimit(
                    Bandwidth.simple(
                        policy.limit.toLong(),
                        policy.duration
                    )
                )
                .build()
        }.tryConsume(1)

}


