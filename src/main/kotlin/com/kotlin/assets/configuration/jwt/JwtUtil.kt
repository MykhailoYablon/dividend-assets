package com.kotlin.assets.configuration.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey
import kotlin.io.encoding.Base64

@Component
class JwtUtil {

    @Value("\${jwt.secret}")
    private lateinit var secret: String

    @Value("\${jwt.expiration}")
    private var expiration: Long = 0

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.decode(secret))
    }

    fun generateToken(username: String, userId: Long): String {
        return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact()
    }

    fun extractUserId(token: String): Long? {
        return runCatching {
            getClaims(token).get("userId", Long::class.java)
        }.getOrNull()
    }

    fun extractUsername(token: String): String? {
        return runCatching { getClaims(token).subject }.getOrNull()
    }

    fun isTokenValid(token: String): Boolean {
        return runCatching {
            val claims = getClaims(token)
            claims.expiration.after(Date())
        }.getOrDefault(false)
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}