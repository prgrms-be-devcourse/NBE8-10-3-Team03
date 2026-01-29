package com.back.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 로컬 캐시 설정
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("auction");
        cacheManager.setCaffeine(Caffeine.newBuilder()
            // 최대 500개 캐싱 (최신 경매 위주, 약 500KB 메모리 사용)
            .maximumSize(500)

            // TTL: 10분 (경매 정보는 자주 변경되지 않음)
            .expireAfterWrite(10, TimeUnit.MINUTES)

            // 캐시 통계 기록 (모니터링용)
            .recordStats()
        );
        return cacheManager;
    }
}

