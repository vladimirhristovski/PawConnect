package com.sorsix.pawconnect.config

import com.sorsix.pawconnect.security.JwtAuthenticationFilter
import com.sorsix.pawconnect.security.RestAuthenticationEntryPoint
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,

    @Value("\${app.cors.allowed-origins}")
    private val allowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors { it.configurationSource(corsConfigurationSource()) }.csrf { it.disable() }.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }.exceptionHandling {
            it.authenticationEntryPoint(restAuthenticationEntryPoint)
        }.authorizeHttpRequests {
            it.requestMatchers(
                "/api/auth/**", "/actuator/health", "/error"
            ).permitAll()
            it.requestMatchers(HttpMethod.GET, "/api/lookups/**").permitAll()
            it.requestMatchers(HttpMethod.GET, "/api/pets/**").permitAll()
            it.requestMatchers(HttpMethod.GET, "/api/businesses/**").permitAll()
            it.requestMatchers(HttpMethod.GET, "/api/listings/mine").authenticated()
            it.requestMatchers(HttpMethod.GET, "/api/listings", "/api/listings/{id}").permitAll()
            it.requestMatchers(HttpMethod.POST, "/api/pet-matcher/**").authenticated()
            it.requestMatchers("/api/admin/**").hasRole("ADMIN")
            it.anyRequest().authenticated()
        }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins.split(",").map { it.trim() }
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}