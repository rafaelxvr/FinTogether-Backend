package com.fintogether.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for rate limiting.
 */
@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
class RateLimitConfig {
    var enabled: Boolean = true
    var login: RateLimitProperties = RateLimitProperties(5, 5, 1)
    var register: RateLimitProperties = RateLimitProperties(3, 3, 1)
    var passwordReset: RateLimitProperties = RateLimitProperties(3, 3, 5)
    var default: RateLimitProperties = RateLimitProperties(100, 100, 1)
}

/**
 * Rate limit properties for a specific endpoint type.
 */
class RateLimitProperties(
    var capacity: Long = 100,
    var refillTokens: Long = 100,
    var refillDurationMinutes: Long = 1
)
