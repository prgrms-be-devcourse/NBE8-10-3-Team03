package com.back.global.rateLimit;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@Getter
@AllArgsConstructor
public enum RateLimitPolicy {

    LOGIN(5, Duration.ofMinutes(1)),
    API_DEFAULT(100, Duration.ofMinutes(1)),
    API_HEAVY(20, Duration.ofMinutes(1));

    private final int limit;
    private final Duration duration;
}

