package com.faforever.userservice.backend.security

import jakarta.enterprise.context.ApplicationScoped
import java.security.MessageDigest
import java.util.*

/**
 * A pretty insecure SHA-256 password encoder.
 */
@ApplicationScoped
class PasswordEncoder {
    private val sha256 = MessageDigest.getInstance("SHA-256")

    fun encode(rawPassword: CharSequence): String =
        HexFormat.of().formatHex(digest(rawPassword))

    fun matches(rawPassword: String, encodedPassword: String): Boolean =
        hashEquals(encodedPassword, encode(rawPassword))

    private fun digest(rawPassword: CharSequence): ByteArray =
        sha256.digest(rawPassword.toString().toByteArray(Charsets.UTF_8))

    fun hashEquals(expected: String, actual: String): Boolean {
        if (expected.length != actual.length) {
            return false
        }

        var result = true
        // Constant time comparison to prevent against timing attacks.
        for (i in expected.indices) {
            result = result and (expected[i].code == actual[i].code)
        }
        return result
    }
}