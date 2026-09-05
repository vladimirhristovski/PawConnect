package com.sorsix.pawconnect.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Duration

@Configuration
class PetMatcherClientConfig(
    @Value("\${pet-matcher.base-url}") private val baseUrl: String,
    @Value("\${pet-matcher.timeout-ms:20000}") private val timeoutMs: Long,
) {
    @Bean
    fun petMatcherRestClient(): RestClient {
        val snakeCaseMapper =
            JsonMapper
                .builder()
                .addModule(KotlinModule.Builder().build())
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build()

        val jsonConverter = JacksonJsonHttpMessageConverter(snakeCaseMapper)

        val requestFactory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(Duration.ofMillis(timeoutMs))
                setReadTimeout(Duration.ofMillis(timeoutMs))
            }

        return RestClient
            .builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .configureMessageConverters { converters ->
                converters.registerDefaults().withJsonConverter(jsonConverter)
            }.build()
    }
}
