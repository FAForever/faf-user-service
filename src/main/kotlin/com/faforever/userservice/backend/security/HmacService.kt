package com.faforever.userservice.backend.security

import jakarta.enterprise.context.ApplicationScoped
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@ApplicationScoped
class HmacService {

    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
    }

    fun generateHmacToken(message: String, secret: String): String {
        val timeStamp = Instant.now().epochSecond
        val hmacEncoded = generateEncodedMessage(message, secret, timeStamp)
        return "$timeStamp-$hmacEncoded"
    }

    private fun generateEncodedMessage(message: String, secret: String, epochSecond: Long): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), HMAC_SHA256))
        val macMessage = (message + epochSecond).toByteArray(StandardCharsets.UTF_8)

        return URLEncoder.encode(
            String(Base64.getEncoder().encode(mac.doFinal(macMessage)), StandardCharsets.UTF_8),
            StandardCharsets.UTF_8,
        )
    }

    fun isValidHmacToken(token: String, expectedMessage: String, secret: String, ttl: Long): Boolean {
        val (epochSecond, encodedMessage) = token.split("-").let {
            if (it.size != 2) {
                return false
            }

            it[0].toLongOrNull() to it[1]
        }

        if (epochSecond == null) {
            return false
        }

        return Instant.now().isBefore(Instant.ofEpochSecond(epochSecond).plusSeconds(ttl)) &&
            generateEncodedMessage(expectedMessage, secret, epochSecond) == encodedMessage
    }
}
