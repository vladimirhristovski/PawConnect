package com.sorsix.pawconnect.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
@Profile("!test")
class GeocodingClientConfig {

    @Bean
    fun nominatimRestClient(): RestClient =
        RestClient.builder()
            .baseUrl("https://nominatim.openstreetmap.org")
            .defaultHeader("User-Agent", "PawConnect/1.0 (vladimir13hristovski@gmail.com)")
            .requestFactory(SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(5000)
                setReadTimeout(5000)
            })
            .build()
}
