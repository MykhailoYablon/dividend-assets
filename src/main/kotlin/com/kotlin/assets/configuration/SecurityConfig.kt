package com.kotlin.assets.configuration

import com.kotlin.assets.configuration.jwt.JwtFilter
import com.kotlin.assets.service.impl.UserDetailsServiceImpl
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.web.filter.OncePerRequestFilter

@Configuration
class SecurityConfig(
    private val jwtFilter: JwtFilter,
    private val userDetailsService: UserDetailsServiceImpl
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        return DaoAuthenticationProvider(userDetailsService).apply {
            setPasswordEncoder(passwordEncoder())
        }
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        // CSRF token stored in a cookie; Thymeleaf's th:action auto-injects it into all forms.
        // withHttpOnlyFalse() so the JS can read XSRF-TOKEN for any future AJAX calls.
        val csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse()

        // CsrfTokenRequestAttributeHandler handles both the _csrf param (Thymeleaf forms)
        // and the X-XSRF-TOKEN header (AJAX).
        val csrfHandler = CsrfTokenRequestAttributeHandler()

        http
            .authenticationProvider(authenticationProvider())
            .csrf { csrf ->
                csrf.csrfTokenRepository(csrfRepo)
                    .csrfTokenRequestHandler(csrfHandler)
                    // Logout CSRF risk is low (attacker can only force a sign-out, not steal data).
                    // Excluding it avoids stateless-session token-timing issues with the cookie.
                    .ignoringRequestMatchers("/logout")
            }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/login", "/error", "/css/**", "/js/**", "/fonts/**").permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint { request, response, _ ->
                    response.sendRedirect("/login")
                }
            }
            .logout { logout ->
                logout
                    .logoutUrl("/logout")
                    .addLogoutHandler { request, response, _ ->
                        val cookie = Cookie("jwt", "").apply {
                            maxAge = 0
                            path = "/"
                            isHttpOnly = true
                        }
                        response.addCookie(cookie)
                    }
                    .logoutSuccessUrl("/login")
            }
            // Must run immediately after CsrfFilter so the deferred token is resolved
            // before Thymeleaf renders the page (otherwise XSRF-TOKEN cookie is never set
            // and the _csrf hidden field in forms won't match on the next POST).
            .addFilterAfter(csrfCookieFilter(), CsrfFilter::class.java)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    /**
     * Forces the lazy CSRF token to be generated on every request, so that
     * Thymeleaf can read it from request attributes and include it in forms.
     */
    private fun csrfCookieFilter(): OncePerRequestFilter {
        return object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain
            ) {
                val csrf = request.getAttribute(CsrfToken::class.java.name)
                if (csrf is CsrfToken) csrf.token // trigger lazy generation
                filterChain.doFilter(request, response)
            }
        }
    }
}
