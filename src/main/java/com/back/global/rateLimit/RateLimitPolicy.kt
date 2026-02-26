package com.back.global.rateLimit

import java.time.Duration

enum class RateLimitPolicy(
    val limit: Int,
    val duration: Duration
) {
    LOGIN(500000000, Duration.ofMinutes(1)),
    API_DEFAULT(10000000, Duration.ofMinutes(1)),
    API_HEAVY(2000000000, Duration.ofMinutes(1))
}