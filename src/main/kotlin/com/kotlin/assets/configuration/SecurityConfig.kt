package com.kotlin.assets.configuration

import com.kotlin.assets.configuration.jwt.JwtFilter
import com.kotlin.assets.entity.User
import com.kotlin.assets.repository.UserRepository
import com.kotlin.assets.service.impl.UserDetailsServiceImpl
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
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
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler
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

    //!!ONLY ONCE
//    @Bean
//    fun seedUser(userRepository: UserRepository, passwordEncoder: PasswordEncoder) =
//        CommandLineRunner {
//            if (userRepository.findByUsername("GoldenApple") == null) {
//                passwordEncoder.encode("firmanfox$")?.let { username ->
//                    userRepository.save(
//                        User(
//                            username = "GoldenApple",
//                            password = username,
//                            role = "ROLE_USER"
//                        )
//                    )
//                }
//            }
//        }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authenticationProvider(authenticationProvider())
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/login", "/error", "/css/**", "/js/**").permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
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
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun csrfCookieFilter(): FilterRegistrationBean<*> {
        val filter = object : OncePerRequestFilter() {
            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain
            ) {
                val csrf = request.getAttribute(CsrfToken::class.java.name)
                if (csrf is CsrfToken) csrf.token // trigger lazy token generation
                filterChain.doFilter(request, response)
            }
        }
        return FilterRegistrationBean(filter).apply {
            order = Ordered.LOWEST_PRECEDENCE
        }
    }

}