package com.faforever.usermanagement.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import javax.validation.constraints.NotBlank

@ConfigurationProperties(prefix = "faf")
@Validated
@ConstructorBinding
data class FafProperties(
    @NotBlank
    val passwordResetUrl: String,
    @NotBlank
    val registerAccountUrl: String,
)
