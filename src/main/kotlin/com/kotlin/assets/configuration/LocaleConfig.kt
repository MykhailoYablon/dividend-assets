package com.kotlin.assets.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor
import java.time.Duration
import java.util.*

@Configuration
class LocaleConfig : WebMvcConfigurer {

    @Bean
    fun localeResolver(): LocaleResolver =
        CookieLocaleResolver("lang").apply {
            val resolver = CookieLocaleResolver()
            resolver.setDefaultLocale(Locale.forLanguageTag("ua"))
            resolver.setCookieMaxAge(Duration.ofDays(1)) // 1 hour
            return resolver
        }

    @Bean
    fun localeChangeInterceptor(): LocaleChangeInterceptor =
        LocaleChangeInterceptor().apply { paramName = "lang" }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }
}
