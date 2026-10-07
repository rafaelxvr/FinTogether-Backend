package com.fintogether.application.service

import com.fintogether.application.dto.ChangePasswordRequest
import com.fintogether.application.dto.UpdateProfileRequest
import com.fintogether.domain.entity.User
import com.fintogether.domain.exception.EntityNotFoundException
import com.fintogether.domain.exception.InvalidOperationException
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.UserPrincipal
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var userService: UserService

    private lateinit var securityContext: SecurityContext
    private lateinit var authentication: Authentication

    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordEncoder = mockk()

        userService = UserService(
            userRepository = userRepository,
            passwordEncoder = passwordEncoder
        )

        // Mock SecurityContext
        securityContext = mockk()
        authentication = mockk()
        SecurityContextHolder.setContext(securityContext)

        every { securityContext.authentication } returns authentication
        every { authentication.principal } returns UserPrincipal(
            userId,
            "user@example.com",
            "password",
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
        SecurityContextHolder.clearContext()
    }

    // ==================== GET CURRENT USER PROFILE TESTS ====================

    @Test
    fun `getCurrentUserProfile should return user profile`() {
        // Given
        val user = createUser(userId, "user@example.com", "hash")

        every { userRepository.findById(userId) } returns user

        // When
        val response = userService.getCurrentUserProfile()

        // Then
        assertNotNull(response)
        assertEquals(userId, response.id)
        assertEquals("Test User", response.name)
        assertEquals("user@example.com", response.email)
        assertEquals("en-US", response.locale)

        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `getCurrentUserProfile should throw EntityNotFoundException when user not found`() {
        // Given
        every { userRepository.findById(userId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            userService.getCurrentUserProfile()
        }
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    fun `updateProfile should update user name and locale`() {
        // Given
        val user = createUser(userId, "user@example.com", "hash")
        val request = UpdateProfileRequest(
            name = "New Name",
            locale = "pt-BR",
            settings = mapOf("theme" to "dark")
        )

        every { userRepository.findById(userId) } returns user
        every { userRepository.save(user) } returns user

        // When
        val response = userService.updateProfile(request)

        // Then
        assertNotNull(response)
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `updateProfile should throw EntityNotFoundException when user not found`() {
        // Given
        val request = UpdateProfileRequest(name = "New Name")

        every { userRepository.findById(userId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            userService.updateProfile(request)
        }
    }

    // ==================== CHANGE PASSWORD TESTS ====================

    @Test
    fun `changePassword should update password when current password matches`() {
        // Given
        val user = createUser(userId, "user@example.com", "encodedOldPassword")
        val request = ChangePasswordRequest(
            currentPassword = "oldPassword",
            newPassword = "newPassword123"
        )

        every { userRepository.findById(userId) } returns user
        every { passwordEncoder.matches("oldPassword", "encodedOldPassword") } returns true
        every { passwordEncoder.encode("newPassword123") } returns "encodedNewPassword"
        every { userRepository.save(user) } returns user

        // When
        userService.changePassword(request)

        // Then
        verify(exactly = 1) { passwordEncoder.matches("oldPassword", "encodedOldPassword") }
        verify(exactly = 1) { passwordEncoder.encode("newPassword123") }
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `changePassword should throw InvalidOperationException for OAuth users`() {
        // Given
        val oauthUser = User(
            email = "oauth@example.com",
            passwordHash = null,  // OAuth users have null password
            name = "OAuth User",
            locale = "en-US",
            id = userId
        )

        val request = ChangePasswordRequest(
            currentPassword = "anything",
            newPassword = "newPassword123"
        )

        every { userRepository.findById(userId) } returns oauthUser

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            userService.changePassword(request)
        }

        assertEquals("OAuth users cannot change password", exception.message)
        verify(exactly = 0) { passwordEncoder.matches(any(), any()) }
    }

    @Test
    fun `changePassword should throw InvalidOperationException when current password is wrong`() {
        // Given
        val user = createUser(userId, "user@example.com", "encodedPassword")
        val request = ChangePasswordRequest(
            currentPassword = "wrongPassword",
            newPassword = "newPassword123"
        )

        every { userRepository.findById(userId) } returns user
        every { passwordEncoder.matches("wrongPassword", "encodedPassword") } returns false

        // When & Then
        val exception = assertThrows<InvalidOperationException> {
            userService.changePassword(request)
        }

        assertEquals("Current password is incorrect", exception.message)
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any<User>()) }
    }

    @Test
    fun `changePassword should throw EntityNotFoundException when user not found`() {
        // Given
        val request = ChangePasswordRequest(
            currentPassword = "old",
            newPassword = "new123"
        )

        every { userRepository.findById(userId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            userService.changePassword(request)
        }
    }

    // ==================== DELETE ACCOUNT TESTS ====================

    @Test
    fun `deleteAccount should delete user account`() {
        // Given
        val user = createUser(userId, "user@example.com", "hash")

        every { userRepository.findById(userId) } returns user
        every { userRepository.delete(user) } just Runs

        // When
        userService.deleteAccount()

        // Then
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { userRepository.delete(user) }
    }

    @Test
    fun `deleteAccount should throw EntityNotFoundException when user not found`() {
        // Given
        every { userRepository.findById(userId) } returns null

        // When & Then
        assertThrows<EntityNotFoundException> {
            userService.deleteAccount()
        }

        verify(exactly = 0) { userRepository.delete(any<User>()) }
    }

    // ==================== HELPER METHODS ====================

    private fun createUser(id: UUID, email: String, passwordHash: String): User {
        return User(
            email = email,
            passwordHash = passwordHash,
            name = "Test User",
            locale = "en-US",
            id = id
        )
    }
}
