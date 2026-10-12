package com.aistudio.remindcare

import com.aistudio.remindcare.util.HashUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class HashUtilsTest {

    @Test
    fun testPbkdf2HashFormat() {
        val hash = HashUtils.hashPin("1234")
        assertTrue("Hash should start with pbkdf2$ prefix", hash.startsWith("pbkdf2$"))
        assertTrue("Hash length should be substantial", hash.length > 20)
    }

    @Test
    fun testVerifyPinSuccess() {
        val pin = "5678"
        val hash = HashUtils.hashPin(pin)
        assertTrue(HashUtils.verifyPin(pin, hash))
    }

    @Test
    fun testVerifyPinFailure() {
        val hash = HashUtils.hashPin("1234")
        assertFalse(HashUtils.verifyPin("9999", hash))
        assertFalse(HashUtils.verifyPin("", hash))
        assertFalse(HashUtils.verifyPin("1234", ""))
    }

    @Test
    fun testLegacySha256MigrationCompatibility() {
        val pin = "4321"
        // Generate legacy SHA-256 hash using the original salt
        val bytes = (pin + "remindcare_salt_v1").toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val legacyHash = md.digest(bytes).fold("") { str, it -> str + "%02x".format(it) }

        // verifyPin must succeed against legacy SHA-256 hash
        assertTrue("verifyPin must accept legacy SHA-256 hash for smooth user migration", 
            HashUtils.verifyPin(pin, legacyHash))
        assertFalse("verifyPin must reject wrong PIN against legacy SHA-256 hash", 
            HashUtils.verifyPin("0000", legacyHash))
    }

    @Test
    fun testBlankPinReturnsEmptyHash() {
        assertEquals("", HashUtils.hashPin(""))
        assertEquals("", HashUtils.hashPin("   "))
    }

    @Test
    fun testDifferentPinsProduceDifferentHashes() {
        val hash1 = HashUtils.hashPin("1111")
        val hash2 = HashUtils.hashPin("2222")
        assertNotEquals(hash1, hash2)
    }
}
