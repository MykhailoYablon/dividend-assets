package com.kotlin.assets.service

import com.kotlin.assets.configuration.jwt.JwtUtil
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class SecurityUtils(private val jwtUtil: JwtUtil) {

    fun getCurrentUserId(request: HttpServletRequest): Long? {
        return request.cookies
            ?.firstOrNull { it.name == "jwt" }
            ?.value
            ?.let { jwtUtil.extractUserId(it) }
    }
}