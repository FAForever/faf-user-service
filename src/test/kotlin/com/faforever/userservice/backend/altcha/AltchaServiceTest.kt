package com.faforever.userservice.backend.altcha

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.security.MessageDigest
import java.util.Base64

@QuarkusTest
class AltchaServiceTest {

    @Inject
    private lateinit var altchaService: AltchaService

    @Inject
    private lateinit var objectMapper: ObjectMapper

    @InjectMock
    private lateinit var challengeRepository: AltchaChallengeRepository

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
        doNothing().whenever(challengeRepository).persist(any<AltchaChallenge>())
        val challenge = altchaService.createChallenge(maxNumber = 100)
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
        doNothing().whenever(challengeRepository).persist(any<AltchaChallenge>())
        val challenge = altchaService.createChallenge(maxNumber = 10)
        val solvedNumber = solveChallenge(challenge)
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
        doNothing().whenever(challengeRepository).persist(any<AltchaChallenge>())
        whenever(challengeRepository.consumeChallenge(any())).thenReturn(true)
        val challenge = altchaService.createChallenge(maxNumber = 10)
        val solvedNumber = solveChallenge(challenge)
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

    @Test
    fun verifyReplayReturnsFalse() {
        doNothing().whenever(challengeRepository).persist(any<AltchaChallenge>())
        whenever(challengeRepository.consumeChallenge(any())).thenReturn(true, false)
        val challenge = altchaService.createChallenge(maxNumber = 10)
        val solvedNumber = solveChallenge(challenge)
        val payload = AltchaPayload(
            algorithm = challenge.algorithm,
            challenge = challenge.challenge,
            number = solvedNumber,
            salt = challenge.salt,
            signature = challenge.signature,
        )
        val encoded = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(payload))
        assertTrue(altchaService.verifyPayload(encoded))
        assertFalse(altchaService.verifyPayload(encoded))
    }

    @Test
    fun verifyExpiredChallengeReturnsFalse() {
        doNothing().whenever(challengeRepository).persist(any<AltchaChallenge>())
        whenever(challengeRepository.consumeChallenge(any())).thenReturn(false)
        val challenge = altchaService.createChallenge(maxNumber = 10)
        val solvedNumber = solveChallenge(challenge)
        val payload = AltchaPayload(
            algorithm = challenge.algorithm,
            challenge = challenge.challenge,
            number = solvedNumber,
            salt = challenge.salt,
            signature = challenge.signature,
        )
        val encoded = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(payload))
        assertFalse(altchaService.verifyPayload(encoded))
    }

    private fun solveChallenge(challenge: AltchaChallengeResponse): Int {
        return (0..challenge.maxnumber).firstOrNull { n ->
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest("${challenge.salt}$n".toByteArray()).joinToString("") { "%02x".format(it) }
            hash == challenge.challenge
        } ?: throw AssertionError("Could not solve challenge in range 0..${challenge.maxnumber}")
    }
}
