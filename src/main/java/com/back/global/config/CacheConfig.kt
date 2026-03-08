package com.back.global.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCache
import org.springframework.cache.support.SimpleCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(): CacheManager {
        val cacheManager = SimpleCacheManager()
        cacheManager.setCaches(
            listOf(
                CaffeineCache(
                    "auction",
                    Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .recordStats()
                        .build()
                ),
                CaffeineCache(
                    "auctionCount",
                    Caffeine.newBuilder()
                        .maximumSize(1_000)
                        .expireAfterWrite(10, TimeUnit.SECONDS)
                        .recordStats()
                        .build()
                ),
                CaffeineCache(
                    "auctionList",
                    Caffeine.newBuilder()
                        .maximumSize(1_000)
                        .expireAfterWrite(30, TimeUnit.SECONDS)
                        .recordStats()
                        .build()
                ),
                CaffeineCache(
                    "postCount",
                    Caffeine.newBuilder()
                        .maximumSize(1_000)
                        .expireAfterWrite(10, TimeUnit.SECONDS)
                        .recordStats()
                        .build()
                )
            ),
        )
        return cacheManager
    }
}
