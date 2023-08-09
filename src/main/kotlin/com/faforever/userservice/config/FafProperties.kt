package com.faforever.userservice.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import jakarta.validation.constraints.NotBlank
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

    fun account(): Account

    fun jwt(): Jwt

    fun recaptcha(): Recaptcha

    interface Jwt {
        fun secret(): String
    }

    interface Recaptcha {
        fun enabled(): Boolean

        @NotBlank
        fun secret(): String

        @NotBlank
        fun siteKey(): String
    }

    interface Account {
        @NotBlank
        fun passwordResetUrl(): String

        @NotBlank
        fun registerAccountUrl(): String

        @NotBlank
        fun accountLinkUrl(): String

        fun registration(): Registration

        fun passwordReset(): PasswordReset

        fun username(): Username

        interface Registration {
            @WithDefault("3600")
            fun linkExpirationSeconds(): Long

            @NotBlank
            fun activationUrlFormat(): String

            @NotBlank
            fun subject(): String

            @NotBlank
            fun activationMailTemplatePath(): String

            @NotBlank
            fun welcomeSubject(): String

            @NotBlank
            fun welcomeMailTemplatePath(): String

            @NotBlank
            fun termsOfServiceUrl(): String

            @NotBlank
            fun privacyStatementUrl(): String

            @NotBlank
            fun rulesUrl(): String
        }

        interface PasswordReset {
            @WithDefault("3600")
            fun linkExpirationSeconds(): Long

            @NotBlank
            fun passwordResetUrlFormat(): String

            @NotBlank
            fun subject(): String

            @NotBlank
            fun mailTemplatePath(): String
        }

        interface Username {
            @WithDefault("30")
            fun minimumDaysBetweenUsernameChange(): Int

            @WithDefault("6")
            fun usernameReservationTimeInMonths(): Long
        }
    }
}
