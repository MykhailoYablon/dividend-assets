package com.kotlin.assets.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class RestConfig {
    @Bean
    fun nbuClient(): RestClient {
        return RestClient.builder()
            .baseUrl("https://bank.gov.ua") // Your API base URL
            .build();
    }
}