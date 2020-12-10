package com.faforever.userservice.security

import org.springframework.security.crypto.codec.Hex.decode
import org.springframework.security.crypto.codec.Hex.encode
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.security.MessageDigest

/**
 * A pretty insecure SHA-256 password encoder.
 */
@Component
class FafPasswordEncoder : PasswordEncoder {
    private val sha256 = MessageDigest.getInstance("SHA-256")

    override fun encode(rawPassword: CharSequence): String =
        String(encode(digest(rawPassword)))

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean =
        matches(decode(encodedPassword), digest(rawPassword))

    private fun digest(rawPassword: CharSequence): ByteArray =
        sha256.digest(rawPassword.toString().toByteArray(Charsets.UTF_8))

    private fun matches(expected: ByteArray, actual: ByteArray): Boolean {
        if (expected.size != actual.size) {
            return false
        }

        var result = 0
        // Constant time comparison to prevent against timing attacks.
        for (i in expected.indices) {
            result = result or (expected[i].toInt() xor actual[i].toInt())
        }
        return result == 0
    }
}
