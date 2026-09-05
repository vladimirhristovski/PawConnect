package com.sorsix.pawconnect.common

fun denialReason(
    allowed: Boolean,
    message: String,
): String? = if (allowed) null else message
