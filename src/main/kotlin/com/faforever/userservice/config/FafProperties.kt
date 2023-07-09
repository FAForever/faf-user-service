package com.faforever.userservice.config

import io.smallrye.config.ConfigMapping
import jakarta.validation.constraints.NotBlank

@ConfigMapping(prefix = "faf")
interface FafProperties {
    fun environment(): String?

    /**
     * Define the header, where to pick the real ip address from. For regular reverse proxies such as nginx or Traefik,
     * this is X-Real-Ip. However, in certain scenarios such as Cloudflare proxy different headers might be required.
     */
    @NotBlank
    fun realIpHeader(): String

    @NotBlank
    fun hydraBaseUrl(): String

    @NotBlank
    fun passwordResetUrl(): String

    @NotBlank
    fun registerAccountUrl(): String

    @NotBlank
    fun accountLinkUrl(): String
}
