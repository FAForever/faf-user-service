package com.faforever.userservice.backend.altcha

import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class AltchaChallengeResponse(
    val algorithm: String,
    val challenge: String,
    val maxnumber: Int,
    val salt: String,
    val signature: String,
)

data class AltchaPayload(
    val algorithm: String,
    val challenge: String,
    val number: Int,
    val salt: String,
    val signature: String,
)

@ApplicationScoped
class AltchaService(
    private val fafProperties: FafProperties,
    private val objectMapper: ObjectMapper,
    private val challengeRepository: AltchaChallengeRepository,
) {
    companion object {
        private const val ALGORITHM = "SHA-256"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val DEFAULT_MAX_NUMBER = 1_000_000
        private const val CHALLENGE_VALIDITY_SECONDS = 600L
        private val secureRandom = SecureRandom()
        val LOG: Logger = LoggerFactory.getLogger(AltchaService::class.java)
    }

    init {
        if (fafProperties.altcha().enabled()) {
            require(fafProperties.altcha().hmacKey().isNotBlank()) {
                "faf.altcha.hmac-key must be set when Altcha is enabled"
            }
        }
    }

    @Transactional
    fun createChallenge(maxNumber: Int = DEFAULT_MAX_NUMBER): AltchaChallengeResponse {
        require(maxNumber > 0) { "maxNumber must be greater than 0" }

        val salt = generateSalt()
        val number = secureRandom.nextInt(maxNumber)
        val challenge = sha256("$salt$number")
        val signature = hmacSha256(challenge, fafProperties.altcha().hmacKey())
        val expiresAt = Instant.now().plusSeconds(CHALLENGE_VALIDITY_SECONDS)

        challengeRepository.persist(AltchaChallenge(challenge = challenge, expiresAt = expiresAt))

        return AltchaChallengeResponse(
            algorithm = ALGORITHM,
            challenge = challenge,
            maxnumber = maxNumber,
            salt = salt,
            signature = signature,
        )
    }

    /**
     * Validates the proof-of-work math and HMAC signature without touching the database.
     * Used by the form binder to enable/disable the submit button as the user fills the form.
     * Does NOT protect against replay attacks — call [verifyPayload] at submit time for that.
     */
    fun isPayloadSignatureValid(payloadBase64: String): Boolean {
        if (!fafProperties.altcha().enabled()) return true
        return parseAndVerifySignature(payloadBase64) != null
    }

    /**
     * Fully verifies the payload: validates math + HMAC and atomically consumes the challenge
     * from the database to prevent replay attacks. Must be called exactly once at submit time.
     */
    @Transactional
    fun verifyPayload(payloadBase64: String): Boolean {
        if (!fafProperties.altcha().enabled()) {
            LOG.debug("Altcha validation is disabled")
            return true
        }

        val payload = parseAndVerifySignature(payloadBase64) ?: return false

        // The Altcha spec is stateless by design: the HMAC signature proves the server issued
        // the challenge, so no server-side storage is needed. However, this does not prevent
        // replay attacks. We therefore persist each issued challenge in the DB and consume it
        // exactly once here. The signature check above is redundant but kept for spec compliance.
        if (!challengeRepository.consumeChallenge(payload.challenge)) {
            LOG.debug("Altcha challenge not found, expired, or already used")
            return false
        }

        LOG.debug("Altcha validation successful")
        return true
    }

    private fun parseAndVerifySignature(payloadBase64: String): AltchaPayload? {
        if (payloadBase64.isBlank()) {
            LOG.debug("Altcha payload is empty")
            return null
        }
        return try {
            val json = String(Base64.getDecoder().decode(payloadBase64))
            val payload = objectMapper.readValue(json, AltchaPayload::class.java)

            if (payload.algorithm != ALGORITHM) {
                LOG.debug("Altcha payload uses unsupported algorithm: {}", payload.algorithm)
                return null
            }

            val expectedChallenge = sha256("${payload.salt}${payload.number}")
            if (expectedChallenge != payload.challenge) {
                LOG.debug("Altcha challenge verification failed")
                return null
            }

            val expectedSignature = hmacSha256(payload.challenge, fafProperties.altcha().hmacKey())
            if (expectedSignature != payload.signature) {
                LOG.debug("Altcha signature verification failed")
                return null
            }

            payload
        } catch (e: Exception) {
            LOG.debug("Altcha payload verification failed", e)
            null
        }
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(message: String, key: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
        return mac.doFinal(message.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(length: Int = 16): String {
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
