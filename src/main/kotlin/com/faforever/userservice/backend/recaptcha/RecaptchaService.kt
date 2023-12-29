package com.faforever.userservice.backend.recaptcha

import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.cache.CacheResult
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

@ApplicationScoped
@RegisterRestClient(configKey = "recaptcha")
interface RecaptchaClient {

    @POST
    @Path("/siteverify")
    fun validateResponse(
        @QueryParam("secret") secret: String,
        @QueryParam("response") response: String?,
    ): VerifyResponse
}

@ApplicationScoped
class RecaptchaService(
    private val fafProperties: FafProperties,
    @RestClient private val recaptchaClient: RecaptchaClient,
) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(RecaptchaService::class.java)
    }

    @CacheResult(cacheName = "recaptcha")
    fun validateResponse(recaptchaResponse: String): Boolean {
        if (!fafProperties.recaptcha().enabled()) {
            LOG.debug("Recaptcha validation is disabled")
            return true
        }

        if (recaptchaResponse.isBlank()) {
            LOG.debug("Recaptcha response is empty")
            return false
        }

        LOG.debug("Validating response: {}", recaptchaResponse)

        val validateResponse = recaptchaClient.validateResponse(fafProperties.recaptcha().secret(), recaptchaResponse)

        if (!validateResponse.success) {
            LOG.debug("Recaptcha validation failed for reasons: {}", validateResponse.errorCodes)
            return false
        }

        LOG.debug("Recaptcha validation successful")
        return true
    }
}

data class VerifyResponse(
    val success: Boolean,
    val challengeTs: OffsetDateTime?,
    val hostname: String,
    @JsonProperty("error-codes") val errorCodes: List<String>?,
)
