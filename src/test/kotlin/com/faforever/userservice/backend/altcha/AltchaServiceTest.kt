package com.faforever.userservice.backend.altcha

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.common.constraint.Assert.assertFalse
import io.smallrye.common.constraint.Assert.assertTrue
import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import java.util.Base64

@QuarkusTest
class AltchaServiceTest {

    @Inject
    private lateinit var altchaService: AltchaService

    @Inject
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun verifyBlankPayloadReturnsFalse() {
        assertFalse(altchaService.verifyPayload(""))
    }

    @Test
    fun verifyMalformedBase64ReturnsFalse() {
        assertFalse(altchaService.verifyPayload("not-valid-base64!!!"))
    }

    @Test
    fun verifyInvalidJsonReturnsFalse() {
        val encoded = Base64.getEncoder().encodeToString("not-json".toByteArray())
        assertFalse(altchaService.verifyPayload(encoded))
    }

    @Test
    fun verifyWrongNumberReturnsFalse() {
        val challenge = altchaService.createChallenge(maxNumber = 100)
        // Use number 0 which almost certainly won't match the challenge
        val payload = AltchaPayload(
            algorithm = challenge.algorithm,
            challenge = challenge.challenge,
            number = challenge.maxnumber + 1,
            salt = challenge.salt,
            signature = challenge.signature,
        )
        val encoded = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(payload))
        assertFalse(altchaService.verifyPayload(encoded))
    }

    @Test
    fun verifyTamperedSignatureReturnsFalse() {
        val challenge = altchaService.createChallenge(maxNumber = 10)
        val solvedNumber = (0..10).firstOrNull { n ->
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest("${challenge.salt}$n".toByteArray()).joinToString("") { "%02x".format(it) }
            hash == challenge.challenge
        } ?: return // skip if not found in range (unlikely with maxNumber=10)

        val payload = AltchaPayload(
            algorithm = challenge.algorithm,
            challenge = challenge.challenge,
            number = solvedNumber,
            salt = challenge.salt,
            signature = "tampered-signature",
        )
        val encoded = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(payload))
        assertFalse(altchaService.verifyPayload(encoded))
    }

    @Test
    fun verifyFullRoundTripSucceeds() {
        val challenge = altchaService.createChallenge(maxNumber = 10)
        val solvedNumber = (0..10).firstOrNull { n ->
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest("${challenge.salt}$n".toByteArray()).joinToString("") { "%02x".format(it) }
            hash == challenge.challenge
        } ?: throw AssertionError("Could not solve challenge in range 0..10")

        val payload = AltchaPayload(
            algorithm = challenge.algorithm,
            challenge = challenge.challenge,
            number = solvedNumber,
            salt = challenge.salt,
            signature = challenge.signature,
        )
        val encoded = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(payload))
        assertTrue(altchaService.verifyPayload(encoded))
    }
}
