package com.jaroapps.dailybrain.feature.profile.domain.model

import java.security.SecureRandom
import java.util.Locale

object InviteCodePolicy {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val CODE_LENGTH = 8
    private const val QR_PREFIX = "dailybrain://invite/"
    private val random = SecureRandom()

    fun normalize(input: String): String =
        input.trim().uppercase(Locale.ROOT).filter { it in ALPHABET }

    fun sanitizeInput(input: String): String = normalize(input).take(CODE_LENGTH)

    fun isValid(input: String): Boolean {
        val normalized = input.uppercase(Locale.ROOT)
        return normalized.length == CODE_LENGTH && normalized.all { it in ALPHABET }
    }

    fun generate(): String = buildString(CODE_LENGTH) {
        repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    fun qrPayload(inviteCode: String): String = QR_PREFIX + normalize(inviteCode)

    fun fromQrPayload(payload: String): String? {
        val value = payload.trim()
        val candidate = if (value.lowercase(Locale.ROOT).startsWith(QR_PREFIX)) {
            value.substring(QR_PREFIX.length)
        } else value
        return normalize(candidate).takeIf(::isValid)
    }
}
