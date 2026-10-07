package com.fintogether.infrastructure.filter

import com.fintogether.infrastructure.config.RateLimitConfig
import com.fintogether.infrastructure.config.RateLimitProperties
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Rate limiting filter using Bucket4j token bucket algorithm.
 * Applies different rate limits based on the endpoint being accessed.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RateLimitFilter(
    private val rateLimitConfig: RateLimitConfig
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(RateLimitFilter::class.java)

    // Caches for storing buckets per IP address
    private val loginBuckets = ConcurrentHashMap<String, Bucket>()
    private val registerBuckets = ConcurrentHashMap<String, Bucket>()
    private val passwordResetBuckets = ConcurrentHashMap<String, Bucket>()
    private val defaultBuckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!rateLimitConfig.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = getClientIp(request)
        val path = request.requestURI
        val method = request.method

        // Skip rate limiting for static resources and swagger
        if (shouldSkipRateLimit(path)) {
            filterChain.doFilter(request, response)
            return
        }

        val bucket = resolveBucket(path, method, clientIp)
        val probe = bucket.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", probe.remainingTokens.toString())
            filterChain.doFilter(request, response)
        } else {
            val waitTimeSeconds = probe.nanosToWaitForRefill / 1_000_000_000
            log.warn("Rate limit exceeded for IP: $clientIp on path: $path")

            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.addHeader("X-Rate-Limit-Remaining", "0")
            response.addHeader("Retry-After", waitTimeSeconds.toString())
            response.writer.write("""
                {
                    "success": false,
                    "message": "Too many requests. Please try again in $waitTimeSeconds seconds.",
                    "data": null
                }
            """.trimIndent())
        }
    }

    /**
     * Get the client IP address, handling proxy scenarios.
     */
    private fun getClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        return if (xForwardedFor != null && xForwardedFor.isNotBlank()) {
            xForwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr
        }
    }

    /**
     * Determine if rate limiting should be skipped for this path.
     */
    private fun shouldSkipRateLimit(path: String): Boolean {
        return path.startsWith("/swagger-ui") ||
                path.startsWith("/api-docs") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/actuator") ||
                path.endsWith(".js") ||
                path.endsWith(".css") ||
                path.endsWith(".html") ||
                path.endsWith(".ico")
    }

    /**
     * Resolve the appropriate bucket based on the endpoint.
     */
    private fun resolveBucket(path: String, method: String, clientIp: String): Bucket {
        return when {
            path == "/api/auth/login" && method == "POST" -> {
                loginBuckets.computeIfAbsent(clientIp) {
                    createBucket(rateLimitConfig.login)
                }
            }
            path == "/api/auth/register" && method == "POST" -> {
                registerBuckets.computeIfAbsent(clientIp) {
                    createBucket(rateLimitConfig.register)
                }
            }
            path == "/api/auth/forgot-password" && method == "POST" -> {
                passwordResetBuckets.computeIfAbsent(clientIp) {
                    createBucket(rateLimitConfig.passwordReset)
                }
            }
            else -> {
                defaultBuckets.computeIfAbsent(clientIp) {
                    createBucket(rateLimitConfig.default)
                }
            }
        }
    }

    /**
     * Create a new bucket with the specified rate limit properties.
     */
    private fun createBucket(properties: RateLimitProperties): Bucket {
        val limit = Bandwidth.builder()
            .capacity(properties.capacity)
            .refillGreedy(properties.refillTokens, Duration.ofMinutes(properties.refillDurationMinutes))
            .build()
        return Bucket.builder()
            .addLimit(limit)
            .build()
    }
}
