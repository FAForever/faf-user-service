package com.faforever.userservice.backend.security

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
class HmacServiceTest {

    @Inject
    private lateinit var hmacService: HmacService

    @Test
    fun testHmacGenerationAndValidation() {
        val message = "message"
        val secret = "secret"
        val token = hmacService.generateHmacToken(message, secret)
        assertTrue(hmacService.isValidHmacToken(token, message, secret, 1))
    }

    @Test
    fun testHmacGenerationAndValidationExpires() {
        val message = "message"
        val secret = "secret"
        val token = hmacService.generateHmacToken(message, secret)

        Thread.sleep(1000)

        assertFalse(hmacService.isValidHmacToken(token, message, secret, 1))
    }

    @Test
    fun testHmacGenerationAndValidationDifferentMessage() {
        val message = "message"
        val secret = "secret"
        val token = hmacService.generateHmacToken(message, secret)

        Thread.sleep(1000)

        assertFalse(hmacService.isValidHmacToken(token, "differentMessage", secret, 1))
    }

    @Test
    fun testHmacGenerationAndValidationMisformattedToken() {
        val message = "message"
        val secret = "secret"
        val token = hmacService.generateHmacToken(message, secret)

        assertFalse(hmacService.isValidHmacToken(token.replace("-", ""), "differentMessage", secret, 1))
    }

    @Test
    fun testHmacGenerationAndValidationBadTimestamp() {
        assertFalse(hmacService.isValidHmacToken("a-b", "differentMessage", "secret", 1))
    }
}
