package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.entity.User
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.external.EmailService
import com.fintogether.infrastructure.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager,
    private val emailService: EmailService
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        logger.info("Registering new user with email: ${request.email}")

        // Check if user already exists
        if (userRepository.existsByEmail(request.email)) {
            logger.warn("Registration failed - email already in use: ${request.email}")
            throw IllegalArgumentException("Email already in use")
        }

        // Create new user
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            name = request.name,
            locale = request.locale
        )

        val savedUser = userRepository.save(user)
        logger.info("User registered successfully: ${savedUser.id}")

        // Send welcome email (async)
        emailService.sendWelcomeEmail(savedUser.email, savedUser.name)

        // Generate tokens
        val accessToken = jwtTokenProvider.generateToken(savedUser.id.toString(), savedUser.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.id.toString(), savedUser.email)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse(
                id = savedUser.id,
                name = savedUser.name,
                email = savedUser.email,
                locale = savedUser.locale
            )
        )
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        logger.info("Login attempt for email: ${request.email}")

        // Authenticate user
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        // Get user
        val user = userRepository.findByEmail(request.email)
            ?: throw EntityNotFoundException("User", request.email)

        logger.info("User logged in successfully: ${user.id}")

        // Generate tokens
        val accessToken = jwtTokenProvider.generateToken(user.id.toString(), user.email)
        val refreshToken = jwtTokenProvider.generateRefreshToken(user.id.toString(), user.email)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = UserResponse(
                id = user.id,
                name = user.name,
                email = user.email,
                locale = user.locale
            )
        )
    }

    fun refreshToken(request: RefreshTokenRequest): AuthResponse {
        logger.debug("Token refresh requested")
        val token = request.refreshToken

        if (!jwtTokenProvider.validateToken(token)) {
            logger.warn("Invalid refresh token provided")
            throw IllegalArgumentException("Invalid refresh token")
        }

        val userId = jwtTokenProvider.getUserIdFromToken(token)
        val user = userRepository.findById(userId)
            ?: throw EntityNotFoundException("User", userId)

        // Generate new tokens
        val accessToken = jwtTokenProvider.generateToken(user.id.toString(), user.email)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(user.id.toString(), user.email)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = newRefreshToken,
            user = UserResponse(
                id = user.id,
                name = user.name,
                email = user.email,
                locale = user.locale
            )
        )
    }

    @Transactional
    fun requestPasswordReset(request: ForgotPasswordRequest) {
        logger.info("Password reset requested for email: ${request.email}")

        val user = userRepository.findByEmail(request.email)
        if (user == null) {
            // Don't reveal whether the email exists - security best practice
            logger.warn("Password reset requested for non-existent email: ${request.email}")
            return
        }

        // Generate a unique reset token
        val resetToken = UUID.randomUUID().toString()
        user.setPasswordResetToken(resetToken)
        userRepository.save(user)

        // Send password reset email (async)
        emailService.sendPasswordResetEmail(user.email, resetToken, user.name)
        logger.info("Password reset email sent to: ${user.email}")
    }

    @Transactional
    fun resetPassword(request: ResetPasswordRequest) {
        logger.info("Password reset attempt with token")

        val user = userRepository.findByResetToken(request.token)
            ?: run {
                logger.warn("Password reset failed - invalid token")
                throw IllegalArgumentException("Invalid or expired reset token")
            }

        if (!user.isResetTokenValid(request.token)) {
            logger.warn("Password reset failed - expired token for user: ${user.id}")
            throw IllegalArgumentException("Invalid or expired reset token")
        }

        // Update password and clear reset token
        user.updatePassword(passwordEncoder.encode(request.newPassword))
        user.clearPasswordResetToken()
        userRepository.save(user)

        logger.info("Password reset successful for user: ${user.id}")
    }
}
