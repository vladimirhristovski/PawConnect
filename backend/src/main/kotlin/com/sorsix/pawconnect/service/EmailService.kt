package com.sorsix.pawconnect.service

interface EmailService {
    fun sendEmail(
        to: String,
        subject: String,
        body: String,
    )
}
