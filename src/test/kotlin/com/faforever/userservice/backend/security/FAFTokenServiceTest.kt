package com.faforever.userservice.backend.security

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasEntry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

@QuarkusTest
class FAFTokenServiceTest {

    @Inject
    private lateinit var fafTokenService: FafTokenService

    @Test
    fun testTokenCreationAndParsing() {
        val attributes = mapOf("username" to "test", "email" to "test@test.com")
        val token = fafTokenService.createToken(FafTokenType.REGISTRATION, Duration.ofSeconds(60), attributes)
        val decodedAttributes = fafTokenService.getTokenClaims(FafTokenType.REGISTRATION, token)
        assertThat(decodedAttributes, hasEntry("username", "test"))
        assertThat(decodedAttributes, hasEntry("email", "test@test.com"))
    }

    @Test
    fun testTokenFailsWithWrongType() {
        val attributes = mapOf("username" to "test", "email" to "test@test.com")
        val token = fafTokenService.createToken(FafTokenType.REGISTRATION, Duration.ofSeconds(60), attributes)
        assertThrows<IllegalArgumentException> {
            fafTokenService.getTokenClaims(
                FafTokenType.LINK_TO_STEAM,
                token
            )
        }
    }

    @Test
    fun testTokenFailsExpired() {
        val attributes = mapOf("username" to "test", "email" to "test@test.com")
        val token = fafTokenService.createToken(FafTokenType.REGISTRATION, Duration.ofSeconds(-60), attributes)
        assertThrows<IllegalArgumentException> {
            fafTokenService.getTokenClaims(
                FafTokenType.REGISTRATION,
                token
            )
        }
    }

    @Test
    fun testTokenFailsCustomAction() {
        val attributes = mapOf("username" to "test", "email" to "test@test.com", "action" to "bad_action")
        assertThrows<IllegalArgumentException> {
            fafTokenService.createToken(FafTokenType.REGISTRATION, Duration.ofSeconds(60), attributes)
        }
    }

}