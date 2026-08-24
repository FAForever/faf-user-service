package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.domain.AccountRequest
import com.faforever.userservice.backend.domain.AccountRequestRepository
import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWEHeader
import com.nimbusds.jose.crypto.AESDecrypter
import com.nimbusds.jose.crypto.AESEncrypter
import com.nimbusds.jwt.EncryptedJWT
import com.nimbusds.jwt.JWTClaimsSet
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.spec.KeySpec
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.TemporalAmount
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.reflect.KClass

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
    private val accountRequestRepository: AccountRequestRepository,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(FafTokenService::class.java)
        private const val KEY_ACTION = "action"

        // jwt claims that are not part of our payload
        private val JWT_META_CLAIMS = setOf(KEY_ACTION, "exp", "iat", "nbf", "sub", "iss", "aud", "jti")
        private val MAP_TYPE = object : TypeReference<Map<String, Any>>() {}
    }

    private val secretKey = SecretKeyGenerator.getKeyFromString(fafProperties.jwt().secret())
    private val jweEncrypter = AESEncrypter(secretKey)
    private val jweDecrypter = AESDecrypter(secretKey)

    @Transactional
    fun createToken(
        token: FafToken,
        lifetime: TemporalAmount,
    ): String =
        when (token) {
            is FafToken.EmailChange,
            is FafToken.AccountDeletion,
            -> createOpaqueToken(token, lifetime)

            is FafToken.Registration,
            is FafToken.PasswordReset,
            is FafToken.LinkToSteam,
            -> createJwtToken(token, lifetime)
        }

    private fun createOpaqueToken(
        token: FafToken,
        lifetime: TemporalAmount,
    ): String {
        val type = token.toType()
        val userId = when (token) {
            is FafToken.EmailChange -> token.userId
            is FafToken.AccountDeletion -> token.userId
            else -> throw IllegalArgumentException("Token type ${type.name} does not support opaque token creation")
        }
        val opaque = UUID.randomUUID().toString()
        accountRequestRepository.deleteByUserIdAndType(userId, type)
        accountRequestRepository.persist(
            AccountRequest(
                id = opaque,
                userId = userId,
                type = type,
                expiresAt = OffsetDateTime.now().plus(lifetime),
                data = objectMapper.convertValue(token, MAP_TYPE),
            ),
        )
        return opaque
    }

    private fun createJwtToken(
        token: FafToken,
        lifetime: TemporalAmount,
    ): String {
        val type = token.toType()
        val jwtBuilder =
            JWTClaimsSet
                .Builder()
                .expirationTime(Date.from(Instant.now().plus(lifetime)))
                .issueTime(Date.from(Instant.now()))
                .claim(KEY_ACTION, type.name)

        objectMapper.convertValue(token, MAP_TYPE).forEach { (key, value) ->
            jwtBuilder.claim(key, value)
        }

        val jwe = EncryptedJWT(JWEHeader(JWEAlgorithm.A256KW, EncryptionMethod.A128CBC_HS256), jwtBuilder.build())
        jwe.encrypt(jweEncrypter)

        return jwe.serialize()
    }

    // single use db token for email change and account deletion
    @Transactional
    fun <T : FafToken> consumeToken(
        expectedType: KClass<T>,
        tokenValue: String,
    ): T {
        val expected = FafTokenType.fromTokenClass(expectedType)
        // only db tokens are consumable
        if (expected != FafTokenType.EMAIL_CHANGE && expected != FafTokenType.ACCOUNT_DELETION) {
            throw IllegalArgumentException("Token type ${expected.name} does not support consumption")
        }

        val request =
            accountRequestRepository.findById(tokenValue)
                ?: throw IllegalArgumentException("Token not found")
        if (request.type != expected) {
            throw IllegalArgumentException("Token does not match expected type")
        }
        if (request.expiresAt.isBefore(OffsetDateTime.now())) {
            throw IllegalArgumentException("Token is expired")
        }

        accountRequestRepository.delete(request)
        return objectMapper.convertValue(request.data, expectedType.java)
    }

    fun <T : FafToken> getToken(
        expectedType: KClass<T>,
        tokenValue: String,
    ): T {
        LOG.debug("Reading token of expected type {}", expectedType.simpleName)

        val jwe = EncryptedJWT.parse(tokenValue)
        jwe.decrypt(jweDecrypter)

        val tokenClaims = jwe.jwtClaimsSet
        if (tokenClaims.expirationTime?.before(Date.from(Instant.now())) == true) {
            throw IllegalArgumentException("Token is expired")
        }

        val expected = FafTokenType.fromTokenClass(expectedType)
        if (tokenClaims.claims[KEY_ACTION] != expected.name) {
            throw IllegalArgumentException("Token does not match expected type")
        }

        val claims = tokenClaims.claims.filterKeys { it !in JWT_META_CLAIMS }
        return objectMapper.convertValue(claims, expectedType.java)
    }
}
