package com.kotlin.assets.service.impl

import com.kotlin.assets.dto.MyUserDetails
import com.kotlin.assets.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found: $username")

        return MyUserDetails(
            id = user.id!!,
            username = user.username,
            password = user.password,
            authorities = listOf(SimpleGrantedAuthority(user.role))
        )
    }
}