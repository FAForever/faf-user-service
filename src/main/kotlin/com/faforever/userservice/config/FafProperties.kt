package com.faforever.userservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@ConfigurationProperties(prefix = "faf")
@Validated
@ConstructorBinding
data class FafProperties(
    val environment: String?,
    @NotBlank
    val passwordResetUrl: String,
    @NotBlank
    val registerAccountUrl: String,
    @NotBlank
    val accountLinkUrl: String,
)
