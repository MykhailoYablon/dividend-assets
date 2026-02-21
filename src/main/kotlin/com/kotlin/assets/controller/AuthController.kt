package com.kotlin.assets.controller

import com.kotlin.assets.configuration.jwt.JwtUtil
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.naming.AuthenticationException

@Controller
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtUtil: JwtUtil
) {
    @GetMapping("/login")
    fun loginPage(): String {
        return "login"
    }

    @PostMapping("/login")
    fun login(
        @RequestParam username: String,
        @RequestParam password: String,
        response: HttpServletResponse
    ): String {
        return try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(username, password)
            )
            val token = jwtUtil.generateToken(authentication.name)

            val cookie = Cookie("jwt", token).apply {
                isHttpOnly = true
                secure = false // set to false for local HTTP dev
                path = "/"
                maxAge = 3600 // matches jwt.expiration in seconds
            }
            response.addCookie(cookie)

            "redirect:/"
        } catch (e: AuthenticationException) {
            "redirect:/login?error"
        }
    }
}