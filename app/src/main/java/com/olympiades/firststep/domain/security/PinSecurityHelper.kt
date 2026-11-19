package com.olympiades.firststep.domain.security

import java.security.MessageDigest
import java.security.SecureRandom

object PinSecurityHelper {
    private const val HASH_ITERATIONS = 1000
    private val PIN_REGEX = Regex("^\\d{4}$")

    fun isValidPinFormat(pin: String): Boolean = PIN_REGEX.matches(pin)

    fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var current = "$salt:$pin".toByteArray(Charsets.UTF_8)
        repeat(HASH_ITERATIONS) {
            current = digest.digest(current)
        }
        return current.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, salt: String, expectedHash: String): Boolean {
        if (!isValidPinFormat(pin) || salt.isBlank() || expectedHash.isBlank()) return false
        val actual = hashPin(pin, salt).toByteArray(Charsets.UTF_8)
        val expected = expectedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(actual, expected)
    }
}
