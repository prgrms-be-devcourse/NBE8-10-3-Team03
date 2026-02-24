package com.back.global.rateLimit

import java.time.Duration

enum class RateLimitPolicy(
    val limit: Int,
    val duration: Duration
) {
    LOGIN(5, Duration.ofMinutes(1)),
    API_DEFAULT(100, Duration.ofMinutes(1)),
    API_HEAVY(20, Duration.ofMinutes(1))
}