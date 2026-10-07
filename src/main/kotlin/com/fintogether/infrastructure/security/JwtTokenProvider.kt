package com.fintogether.infrastructure.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val jwtSecret: String,

    @Value("\${jwt.expiration}")
    private val jwtExpiration: Long,

    @Value("\${jwt.refresh-expiration}")
    private val jwtRefreshExpiration: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        return generateToken(userPrincipal.id.toString(), userPrincipal.username, jwtExpiration)
    }

    fun generateToken(userId: String, email: String): String {
        return generateToken(userId, email, jwtExpiration)
    }

    fun generateRefreshToken(userId: String, email: String): String {
        return generateToken(userId, email, jwtRefreshExpiration)
    }

    private fun generateToken(userId: String, email: String, expiration: Long): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId)
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getUserIdFromToken(token: String): UUID {
        val claims = getClaimsFromToken(token)
        return UUID.fromString(claims.subject)
    }

    fun getEmailFromToken(token: String): String {
        val claims = getClaimsFromToken(token)
        return claims["email"] as String
    }

    fun validateToken(token: String): Boolean {
        try {
            getClaimsFromToken(token)
            return true
        } catch (ex: JwtException) {
            // Log the exception
            return false
        } catch (ex: IllegalArgumentException) {
            return false
        }
    }

    private fun getClaimsFromToken(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
