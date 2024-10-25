package com.faforever.userservice.backend.cloudflare

import com.faforever.userservice.backend.security.HmacService
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

@QuarkusTest
class CloudflareServiceTest {
    @Inject
    private lateinit var cloudflareService: CloudflareService

    @Inject
    private lateinit var hmacService: HmacService

    @Test
    fun testCloudFlareToken() {
        val secret = "secret"
        val token = cloudflareService.generateCloudFlareHmacToken(URI.create("http://localhost/test"), secret)
        assertTrue(hmacService.isValidHmacToken(token, "/test", secret, 1))
    }

    @Test
    fun testCloudFlareTokenEmptyPath() {
        val secret = "secret"
        val token = cloudflareService.generateCloudFlareHmacToken(URI.create("http://localhost"), secret)
        assertTrue(hmacService.isValidHmacToken(token, "/", secret, 1))
    }
}
