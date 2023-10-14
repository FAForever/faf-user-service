package com.faforever.userservice.config

import io.smallrye.config.ConfigMapping
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.net.URI
import java.util.*

@ConfigMapping(prefix = "faf")
interface FafProperties {
    fun environment(): Optional<String>

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

    fun lobby(): Lobby

    interface Lobby {
        @NotBlank
        fun secret(): String
        @NotBlank
        fun accessParam(): String
        @NotNull
        fun accessUri(): URI
    }
}
