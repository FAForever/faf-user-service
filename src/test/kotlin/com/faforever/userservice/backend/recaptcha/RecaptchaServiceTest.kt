package com.faforever.userservice.backend.recaptcha

import com.faforever.userservice.config.FafProperties
import io.quarkus.cache.Cache
import io.quarkus.cache.CacheName
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.common.constraint.Assert.assertFalse
import io.smallrye.common.constraint.Assert.assertTrue
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration

@QuarkusTest
class RecaptchaServiceTest {

    @Inject
    private lateinit var recaptchaService: RecaptchaService

    @Inject
    private lateinit var fafProperties: FafProperties

    @InjectMock
    @RestClient
    private lateinit var recaptchaClient: RecaptchaClient

    @Inject
    @CacheName("recaptcha")
    private lateinit var cache: Cache

    @BeforeEach
    fun setup() {
        cache.invalidateAll().await().atMost(Duration.ofSeconds(1))
    }

    @Test
    fun validateRecaptchaBlank() {
        assertFalse(recaptchaService.validateResponse(""))

        verify(recaptchaClient, never()).validateResponse(any(), any())
    }

    @Test
    fun validateRecaptchaInvalid() {
        whenever(recaptchaClient.validateResponse(any(), any())).thenReturn(
            VerifyResponse(success = false, challengeTs = null, hostname = "localhost", errorCodes = null),
        )

        assertFalse(recaptchaService.validateResponse("a"))

        verify(recaptchaClient).validateResponse(any(), any())
    }

    @Test
    fun validateRecaptchaValid() {
        whenever(recaptchaClient.validateResponse(any(), any())).thenReturn(
            VerifyResponse(success = true, challengeTs = null, hostname = "localhost", errorCodes = null),
        )

        assertTrue(recaptchaService.validateResponse("a"))

        verify(recaptchaClient).validateResponse(any(), any())
    }

    @Test
    fun validateRecaptchaCache() {
        whenever(recaptchaClient.validateResponse(any(), any())).thenReturn(
            VerifyResponse(success = true, challengeTs = null, hostname = "localhost", errorCodes = null),
        )

        assertTrue(recaptchaService.validateResponse("a"))
        assertTrue(recaptchaService.validateResponse("a"))

        verify(recaptchaClient).validateResponse(any(), any())
    }
}
