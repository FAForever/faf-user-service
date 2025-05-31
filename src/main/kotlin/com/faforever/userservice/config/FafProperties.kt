package com.faforever.userservice.config

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithDefault
import io.smallrye.config.WithName
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.net.URI
import java.util.*

@ConfigMapping(prefix = "faf")
interface FafProperties {
    @NotBlank
    fun selfUrl(): String

    fun environment(): Optional<String>

    /**
     * Define the header, where to pick the real ip address from. For regular reverse proxies such as nginx or Traefik,
     * this is X-Real-Ip. However, in certain scenarios such as Cloudflare proxy different headers might be required.
     */
    @NotBlank
    fun realIpHeader(): String

    @NotBlank
    fun hydraBaseUrl(): String

    fun account(): Account

    fun jwt(): Jwt

    fun recaptcha(): Recaptcha

    fun lobby(): CloudflareHmacConfig

    fun replay(): CloudflareHmacConfig

    fun chat(): CloudflareHmacConfig

    fun irc(): Irc

    fun steam(): Steam

    interface CloudflareHmacConfig {
        @NotBlank
        fun secret(): String

        @NotBlank
        fun accessParam(): String

        @NotNull
        fun accessUri(): URI
    }

    interface Irc {
        @WithName("fixed.users")
        fun fixedUsers(): Map<String, String>

        fun secret(): String

        fun tokenTtl(): Long
    }

    interface Jwt {
        fun secret(): String

        fun hmac(): Hmac?
    }

    interface Hmac {
        fun message(): String

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
            fun emailTakenMailTemplatePath(): String

            @NotBlank
            fun emailTakenSubject(): String

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
            fun passwordResetInitiateEmailUrlFormat(): String

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

    interface Steam {
        fun loginUrlFormat(): String = "https://steamcommunity.com/openid/login"

        @NotBlank
        fun realm(): String
    }
}
