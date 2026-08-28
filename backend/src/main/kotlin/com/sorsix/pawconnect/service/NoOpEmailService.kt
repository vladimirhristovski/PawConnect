package com.sorsix.pawconnect.service

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["app.mail.enabled"], havingValue = "false")
class NoOpEmailService : EmailService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendEmail(to: String, subject: String, body: String) {
        log.info("Email would be sent to: {}, subject: {}", to, subject)
    }
}