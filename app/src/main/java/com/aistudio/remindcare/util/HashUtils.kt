package com.aistudio.remindcare.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object HashUtils {
    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private const val STATIC_SALT = "remindcare_hardened_salt_v2_elderly_care"
    private const val LEGACY_SALT = "remindcare_salt_v1"

    /**
     * Hashes a PIN using PBKDF2WithHmacSHA256 with 10,000 iterations
     */
    fun hashPin(pin: String): String {
        if (pin.isBlank()) return ""
        val spec = PBEKeySpec(pin.toCharArray(), STATIC_SALT.toByteArray(), ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return "pbkdf2$" + hash.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Verifies an input PIN against stored hash with backward compatibility for legacy SHA-256 hashes
     */
    fun verifyPin(inputPin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        if (inputPin.isBlank()) return false

        if (storedHash.startsWith("pbkdf2$")) {
            return hashPin(inputPin) == storedHash
        }

        // Legacy SHA-256 fallback
        val legacyHash = hashLegacyPin(inputPin)
        return legacyHash == storedHash
    }

    private fun hashLegacyPin(pin: String): String {
        if (pin.isBlank()) return ""
        val bytes = (pin + LEGACY_SALT).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
