package com.fintogether.application.service

import com.fintogether.application.dto.ChangePasswordRequest
import com.fintogether.application.dto.UpdateProfileRequest
import com.fintogether.application.dto.UserResponse
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    @Transactional(readOnly = true)
    fun getCurrentUserProfile(): UserResponse {
        val currentUser = getCurrentUser()
        logger.debug("Getting profile for user: ${currentUser.id}")

        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        return UserResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            locale = user.locale
        )
    }

    @Transactional
    fun updateProfile(request: UpdateProfileRequest): UserResponse {
        val currentUser = getCurrentUser()
        logger.info("Updating profile for user: ${currentUser.id}")

        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        user.updateProfile(
            name = request.name,
            locale = request.locale,
            settings = request.settings
        )

        val updatedUser = userRepository.save(user)
        logger.info("Profile updated successfully for user: ${currentUser.id}")

        return UserResponse(
            id = updatedUser.id,
            name = updatedUser.name,
            email = updatedUser.email,
            locale = updatedUser.locale
        )
    }

    @Transactional
    fun changePassword(request: ChangePasswordRequest) {
        val currentUser = getCurrentUser()
        logger.info("Password change requested for user: ${currentUser.id}")

        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        if (user.isOAuthUser()) {
            logger.warn("OAuth user ${currentUser.id} attempted to change password")
            throw InvalidOperationException("OAuth users cannot change password", "changePassword")
        }

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            logger.warn("Invalid current password for user: ${currentUser.id}")
            throw InvalidOperationException("Current password is incorrect", "changePassword")
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword))
        userRepository.save(user)
        logger.info("Password changed successfully for user: ${currentUser.id}")
    }

    @Transactional
    fun deleteAccount() {
        val currentUser = getCurrentUser()
        logger.info("Account deletion requested for user: ${currentUser.id}")

        val user = userRepository.findById(currentUser.id)
            ?: throw EntityNotFoundException("User", currentUser.id)

        userRepository.delete(user)
        logger.info("Account deleted successfully for user: ${currentUser.id}")
    }

    private fun getCurrentUser(): UserPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.principal as UserPrincipal
    }
}
