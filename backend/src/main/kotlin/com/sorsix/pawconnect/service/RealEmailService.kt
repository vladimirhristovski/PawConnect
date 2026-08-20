package com.sorsix.pawconnect.service

import jakarta.mail.internet.MimeMessage
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

@Service
@ConditionalOnProperty(name = ["app.email.enabled"], havingValue = "true", matchIfMissing = true)
class RealEmailService(
    private val mailSender: JavaMailSender
) : EmailService {

    override fun sendEmail(to: String, subject: String, body: String) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, StandardCharsets.UTF_8.name())
        helper.setTo(to)
        helper.setSubject(subject)
        helper.setText(body, false)
        mailSender.send(message)
    }
}