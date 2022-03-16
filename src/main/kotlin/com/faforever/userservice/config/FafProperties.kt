package com.faforever.userservice.config

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import javax.validation.constraints.NotBlank

@ConfigurationProperties("faf")
@Context
interface FafProperties {
    val environment: String?

    @get:NotBlank
    val hydraBaseUrl: String

    @get:NotBlank
    val passwordResetUrl: String

    @get:NotBlank
    val registerAccountUrl: String

    @get:NotBlank
    val accountLinkUrl: String
}
