package com.faforever.userservice.backend.security

import com.faforever.userservice.config.FafProperties
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.crypto.AESDecrypter
import com.nimbusds.jose.crypto.AESEncrypter
import com.nimbusds.jwt.EncryptedJWT
import com.nimbusds.jwt.JWTClaimsSet
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.spec.KeySpec
import java.text.MessageFormat
import java.time.Instant
import java.time.temporal.TemporalAmount
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

enum class FafTokenType {
    REGISTRATION,
    PASSWORD_RESET,
    LINK_TO_STEAM,
}

class SecretKeyGenerator {
    companion object {
        fun getKeyFromString(salt: String): SecretKey {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec: KeySpec = PBEKeySpec(null, salt.toByteArray(), 262144, 256)
            return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        }
    }
}

@ApplicationScoped
class FafTokenService(
    fafProperties: FafProperties,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(FafTokenService::class.java)
        private const val KEY_ACTION = "action"
    }

    private val secretKey = SecretKeyGenerator.getKeyFromString(fafProperties.jwt().secret())
    private val jweEncrypter = AESEncrypter(secretKey)
    private val jweDecrypter = AESDecrypter(secretKey)

    fun createToken(fafTokenType: FafTokenType, lifetime: TemporalAmount, attributes: Map<String, String>): String {
        if (attributes.containsKey(KEY_ACTION)) {
            throw IllegalArgumentException(
                MessageFormat.format("'{0}' is a protected attributed and must not be used", KEY_ACTION),
            )
        }

        val jwtBuilder = JWTClaimsSet.Builder()
            .expirationTime(Date.from(Instant.now().plus(lifetime)))
            .issueTime(Date.from(Instant.now()))

        jwtBuilder.claim(KEY_ACTION, fafTokenType.name)
        attributes.forEach { (key, value) -> jwtBuilder.claim(key, value) }

        val jwe = EncryptedJWT(JWEHeader(JWEAlgorithm.A256KW, EncryptionMethod.A128CBC_HS256), jwtBuilder.build())
        jwe.encrypt(jweEncrypter)

        return jwe.serialize()
    }

    fun getTokenClaims(fafTokenType: FafTokenType, tokenValue: String): Map<String, String> {
        LOG.debug("Reading token of expected type {}", fafTokenType.name)

        val jwe = EncryptedJWT.parse(tokenValue)
        jwe.decrypt(jweDecrypter)

        val tokenClaims = jwe.jwtClaimsSet
        if (tokenClaims.expirationTime?.before(Date.from(Instant.now())) == true) {
            throw IllegalArgumentException("Token is expired")
        }
        if (tokenClaims?.claims?.get(KEY_ACTION) != fafTokenType.name) {
            throw IllegalArgumentException("Token does not match expected type")
        }
        return tokenClaims.claims.filterKeys { it != KEY_ACTION } as Map<String, String>
    }
}
