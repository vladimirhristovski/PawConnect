package com.sorsix.pawconnect.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import kotlin.test.assertEquals

class RealEmailServiceTest {

    private val mailSender = mockk<JavaMailSender>()
    private val service = RealEmailService(mailSender, "noreply@pawconnect.test")

    @Test
    fun `sendEmail sets from, to and subject then sends the message`() {
        every { mailSender.createMimeMessage() } returns JavaMailSenderImpl().createMimeMessage()
        val sent = slot<MimeMessage>()
        every { mailSender.send(capture(sent)) } returns Unit

        service.sendEmail("applicant@mail.test", "Password Reset Request", "Click the link")

        verify { mailSender.send(any<MimeMessage>()) }
        assertEquals("Password Reset Request", sent.captured.subject)
        assertEquals("noreply@pawconnect.test", sent.captured.from.single().toString())
        assertEquals("applicant@mail.test", sent.captured.allRecipients.single().toString())
    }
}
