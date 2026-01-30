package com.back.global.rateLimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import org.springframework.stereotype.Component;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bandwidth;

import java.util.concurrent.TimeUnit;

@Component
public class Bucket4jRateLimiter {

    private final Cache<String, Bucket> cache;

    public Bucket4jRateLimiter() {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(100_000)
                .build();
    }

    public boolean tryConsume(String key, RateLimitPolicy policy) {
        Bucket bucket = cache.get(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.simple(
                                policy.getLimit(),
                                policy.getDuration()
                        ))
                        .build()
        );

        return bucket.tryConsume(1);
    }
}

