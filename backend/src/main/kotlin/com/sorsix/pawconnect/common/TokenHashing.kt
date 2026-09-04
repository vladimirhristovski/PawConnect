package com.sorsix.pawconnect.common

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.HexFormat

fun sha256Hex(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
    return HexFormat.of().formatHex(digest)
}
