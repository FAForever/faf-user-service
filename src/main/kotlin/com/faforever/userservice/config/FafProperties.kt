package com.faforever.userservice.config

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import javax.validation.constraints.NotBlank

@ConfigurationProperties("faf")
@Context
interface FafProperties {
    val environment: String?

    /**
     * Define the header, where to pick the real ip address from. For regular reverse proxies such as nginx or Traefik,
     * this is X-Real-Ip. However, in certain scenarios such as Cloudflare proxy different headers might be required.
     */
    @get:NotBlank
    val realIpHeader: String

    @get:NotBlank
    val hydraBaseUrl: String

    @get:NotBlank
    val passwordResetUrl: String

    @get:NotBlank
    val registerAccountUrl: String

    @get:NotBlank
    val accountLinkUrl: String
}
