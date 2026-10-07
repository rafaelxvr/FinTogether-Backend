package com.fintogether.application.service

import com.fintogether.application.dto.*
import com.fintogether.domain.entity.User
import com.fintogether.domain.repository.UserRepository
import com.fintogether.infrastructure.security.JwtTokenProvider
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.*

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenProvider: JwtTokenProvider
    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        passwordEncoder = mockk()
        jwtTokenProvider = mockk()
        authenticationManager = mockk()

        authService = AuthService(
            userRepository = userRepository,
            passwordEncoder = passwordEncoder,
            jwtTokenProvider = jwtTokenProvider,
            authenticationManager = authenticationManager
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ==================== REGISTER TESTS ====================

    @Test
    fun `register should create new user and return auth response when email is not in use`() {
        // Given
        val registerRequest = RegisterRequest(
            email = "test@example.com",
            password = "password123",
            name = "Test User",
            locale = "en-US"
        )

        val userId = UUID.randomUUID()
        val encodedPassword = "encodedPassword123"
        val savedUser = User(
            email = registerRequest.email,
            passwordHash = encodedPassword,
            name = registerRequest.name,
            locale = registerRequest.locale,
            id = userId
        )

        val accessToken = "access.token.jwt"
        val refreshToken = "refresh.token.jwt"

        every { userRepository.existsByEmail(registerRequest.email) } returns false
        every { passwordEncoder.encode(registerRequest.password) } returns encodedPassword
        every { userRepository.save(any<User>()) } returns savedUser
        every { jwtTokenProvider.generateToken(savedUser.id.toString(), savedUser.email) } returns accessToken
        every { jwtTokenProvider.generateRefreshToken(savedUser.id.toString(), savedUser.email) } returns refreshToken

        // When
        val response = authService.register(registerRequest)

        // Then
        assertNotNull(response)
        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
        assertEquals(savedUser.id, response.user.id)
        assertEquals(savedUser.name, response.user.name)
        assertEquals(savedUser.email, response.user.email)
        assertEquals(savedUser.locale, response.user.locale)

        verify(exactly = 1) { userRepository.existsByEmail(registerRequest.email) }
        verify(exactly = 1) { passwordEncoder.encode(registerRequest.password) }
        verify(exactly = 1) { userRepository.save(any<User>()) }
        verify(exactly = 1) { jwtTokenProvider.generateToken(savedUser.id.toString(), savedUser.email) }
        verify(exactly = 1) { jwtTokenProvider.generateRefreshToken(savedUser.id.toString(), savedUser.email) }
    }

    @Test
    fun `register should throw IllegalArgumentException when email already exists`() {
        // Given
        val registerRequest = RegisterRequest(
            email = "existing@example.com",
            password = "password123",
            name = "Test User",
            locale = "en-US"
        )

        every { userRepository.existsByEmail(registerRequest.email) } returns true

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.register(registerRequest)
        }

        assertEquals("Email already in use", exception.message)

        verify(exactly = 1) { userRepository.existsByEmail(registerRequest.email) }
        verify(exactly = 0) { passwordEncoder.encode(any()) }
        verify(exactly = 0) { userRepository.save(any<User>()) }
    }

    // ==================== LOGIN TESTS ====================

    @Test
    fun `login should authenticate user and return auth response when credentials are valid`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )

        val userId = UUID.randomUUID()
        val user = User(
            email = loginRequest.email,
            passwordHash = "encodedPassword",
            name = "Test User",
            locale = "en-US",
            id = userId
        )

        val authentication = mockk<Authentication>()
        val accessToken = "access.token.jwt"
        val refreshToken = "refresh.token.jwt"

        every {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)
            )
        } returns authentication
        every { userRepository.findByEmail(loginRequest.email) } returns user
        every { jwtTokenProvider.generateToken(user.id.toString(), user.email) } returns accessToken
        every { jwtTokenProvider.generateRefreshToken(user.id.toString(), user.email) } returns refreshToken

        // When
        val response = authService.login(loginRequest)

        // Then
        assertNotNull(response)
        assertEquals(accessToken, response.accessToken)
        assertEquals(refreshToken, response.refreshToken)
        assertEquals(user.id, response.user.id)
        assertEquals(user.name, response.user.name)
        assertEquals(user.email, response.user.email)

        verify(exactly = 1) { authenticationManager.authenticate(any()) }
        verify(exactly = 1) { userRepository.findByEmail(loginRequest.email) }
        verify(exactly = 1) { jwtTokenProvider.generateToken(user.id.toString(), user.email) }
        verify(exactly = 1) { jwtTokenProvider.generateRefreshToken(user.id.toString(), user.email) }
    }

    @Test
    fun `login should throw BadCredentialsException when credentials are invalid`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "wrongpassword"
        )

        every {
            authenticationManager.authenticate(any())
        } throws BadCredentialsException("Invalid credentials")

        // When & Then
        assertThrows<BadCredentialsException> {
            authService.login(loginRequest)
        }

        verify(exactly = 1) { authenticationManager.authenticate(any()) }
        verify(exactly = 0) { userRepository.findByEmail(any()) }
    }

    @Test
    fun `login should throw IllegalArgumentException when user not found after authentication`() {
        // Given
        val loginRequest = LoginRequest(
            email = "test@example.com",
            password = "password123"
        )

        val authentication = mockk<Authentication>()

        every { authenticationManager.authenticate(any()) } returns authentication
        every { userRepository.findByEmail(loginRequest.email) } returns null

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.login(loginRequest)
        }

        assertEquals("User not found", exception.message)

        verify(exactly = 1) { authenticationManager.authenticate(any()) }
        verify(exactly = 1) { userRepository.findByEmail(loginRequest.email) }
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    fun `refreshToken should generate new tokens when refresh token is valid`() {
        // Given
        val userId = UUID.randomUUID()
        val oldRefreshToken = "old.refresh.token"
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = oldRefreshToken)

        val user = User(
            email = "test@example.com",
            passwordHash = "encodedPassword",
            name = "Test User",
            locale = "en-US",
            id = userId
        )

        val newAccessToken = "new.access.token"
        val newRefreshToken = "new.refresh.token"

        every { jwtTokenProvider.validateToken(oldRefreshToken) } returns true
        every { jwtTokenProvider.getUserIdFromToken(oldRefreshToken) } returns userId
        every { userRepository.findById(userId) } returns user
        every { jwtTokenProvider.generateToken(user.id.toString(), user.email) } returns newAccessToken
        every { jwtTokenProvider.generateRefreshToken(user.id.toString(), user.email) } returns newRefreshToken

        // When
        val response = authService.refreshToken(refreshTokenRequest)

        // Then
        assertNotNull(response)
        assertEquals(newAccessToken, response.accessToken)
        assertEquals(newRefreshToken, response.refreshToken)
        assertEquals(user.id, response.user.id)
        assertEquals(user.name, response.user.name)

        verify(exactly = 1) { jwtTokenProvider.validateToken(oldRefreshToken) }
        verify(exactly = 1) { jwtTokenProvider.getUserIdFromToken(oldRefreshToken) }
        verify(exactly = 1) { userRepository.findById(userId) }
        verify(exactly = 1) { jwtTokenProvider.generateToken(user.id.toString(), user.email) }
        verify(exactly = 1) { jwtTokenProvider.generateRefreshToken(user.id.toString(), user.email) }
    }

    @Test
    fun `refreshToken should throw IllegalArgumentException when token is invalid`() {
        // Given
        val invalidToken = "invalid.token"
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = invalidToken)

        every { jwtTokenProvider.validateToken(invalidToken) } returns false

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.refreshToken(refreshTokenRequest)
        }

        assertEquals("Invalid refresh token", exception.message)

        verify(exactly = 1) { jwtTokenProvider.validateToken(invalidToken) }
        verify(exactly = 0) { jwtTokenProvider.getUserIdFromToken(any()) }
        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    fun `refreshToken should throw IllegalArgumentException when user not found`() {
        // Given
        val userId = UUID.randomUUID()
        val refreshToken = "valid.refresh.token"
        val refreshTokenRequest = RefreshTokenRequest(refreshToken = refreshToken)

        every { jwtTokenProvider.validateToken(refreshToken) } returns true
        every { jwtTokenProvider.getUserIdFromToken(refreshToken) } returns userId
        every { userRepository.findById(userId) } returns null

        // When & Then
        val exception = assertThrows<IllegalArgumentException> {
            authService.refreshToken(refreshTokenRequest)
        }

        assertEquals("User not found", exception.message)

        verify(exactly = 1) { jwtTokenProvider.validateToken(refreshToken) }
        verify(exactly = 1) { jwtTokenProvider.getUserIdFromToken(refreshToken) }
        verify(exactly = 1) { userRepository.findById(userId) }
    }

}
