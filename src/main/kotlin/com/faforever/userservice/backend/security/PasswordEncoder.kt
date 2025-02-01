package com.faforever.userservice.backend.security

import jakarta.enterprise.context.ApplicationScoped
import org.bouncycastle.crypto.generators.BCrypt
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.HexFormat

/**
 * A more secure password encoder that uses SHA-256 combined with bcrypt.
 */
@ApplicationScoped
class PasswordEncoder {
    private val sha256: MessageDigest = MessageDigest.getInstance("SHA-256")
    private val secureRandom = SecureRandom()

    fun encode(rawPassword: CharSequence): String {
        val sha256Hash = digest(rawPassword)
        val salt = generateSalt()
        val bcryptHash = BCrypt.generate(sha256Hash, salt, 12)
        return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(bcryptHash)
    }

    fun matches(rawPassword: String, encodedPassword: String): Boolean {
        val sha256Hash = digest(rawPassword)
        val parts = encodedPassword.split(":")
        if (parts.size != 2) return false

        val salt = HexFormat.of().parseHex(parts[0])
        val storedHash = HexFormat.of().parseHex(parts[1])
        val computedHash = BCrypt.generate(sha256Hash, salt, 12)

        return storedHash.contentEquals(computedHash)
    }

    private fun digest(rawPassword: CharSequence): ByteArray =
        sha256.digest(rawPassword.toString().toByteArray(Charsets.UTF_8))

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt
    }
}
