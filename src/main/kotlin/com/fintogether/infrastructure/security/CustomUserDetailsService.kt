package com.fintogether.infrastructure.security

import com.fintogether.domain.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")

        return UserPrincipal.create(user)
    }

    fun loadUserById(id: UUID): UserDetails {
        val user = userRepository.findById(id)
            ?: throw UsernameNotFoundException("User not found with id: $id")

        return UserPrincipal.create(user)
    }
}
