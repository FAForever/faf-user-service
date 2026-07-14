package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.metrics.MetricHelper
import com.faforever.userservice.backend.security.FafToken
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.steam.SteamService
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

@ApplicationScoped
class RecoveryService(
    private val fafProperties: FafProperties,
    private val metricHelper: MetricHelper,
    private val userRepository: UserRepository,
    private val fafTokenService: FafTokenService,
    private val steamService: SteamService,
    private val emailService: EmailService,
    private val loginService: LoginService,
) {
    enum class Type {
        EMAIL,
        STEAM,
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(RecoveryService::class.java)
    }

    fun requestPasswordResetViaEmail(usernameOrEmail: String) {
        metricHelper.incrementPasswordResetViaEmailRequestCounter()
        val user = userRepository.findByUsernameOrEmail(usernameOrEmail)

        if (user == null) {
            metricHelper.incrementPasswordResetViaEmailFailedCounter()
            LOG.info("No user found for recovery with username/email: {}", usernameOrEmail)
        } else {
            val token = fafTokenService.createToken(
                FafToken.PasswordReset(userId = user.id!!),
                Duration.ofSeconds(fafProperties.account().passwordReset().linkExpirationSeconds()),
            )
            val passwordResetUrl = fafProperties.account().passwordReset().passwordResetUrlFormat().format(token)
            emailService.sendPasswordResetMail(user.username, user.email, passwordResetUrl)
            metricHelper.incrementPasswordResetViaEmailSentCounter()
        }
    }

    fun buildSteamLoginUrl() =
        steamService.buildLoginUrl(
            redirectUrl =
            fafProperties.account().passwordReset().passwordResetUrlFormat().format("STEAM"),
        )

    sealed interface ParsingResult {
        data class ExtractedUser(val type: Type, val user: User) : ParsingResult
        data class ValidNoUser(val type: Type) : ParsingResult
        data class Invalid(val cause: Exception) : ParsingResult
    }

    fun parseRecoveryHttpRequest(parameters: Map<String, List<String>>): ParsingResult {
        // At first glance it may seem strange that a service is parsing http request parameters,
        // but the parameters of the request are determined by this service itself in the request reset phase!

        val token = parameters["token"]?.firstOrNull()
        LOG.debug("Extracted token: {}", token)
        return when (token) {
            null -> {
                metricHelper.incrementPasswordResetViaEmailFailedCounter()
                ParsingResult.Invalid(InvalidRecoveryException("Could not extract token"))
            }
            "STEAM" -> when (val result = steamService.parseSteamIdFromRequestParameters(parameters)) {
                is SteamService.ParsingResult.NoSteamIdPresent,
                is SteamService.ParsingResult.InvalidRedirect,
                -> {
                    metricHelper.incrementPasswordResetViaSteamFailedCounter()
                    ParsingResult.Invalid(
                        InvalidRecoveryException("Steam based recovery attempt is invalid"),
                    )
                }
                is SteamService.ParsingResult.ExtractedId -> {
                    val user = steamService.findUserBySteamId(result.steamId)
                    if (user == null) {
                        metricHelper.incrementPasswordResetViaSteamFailedCounter()
                        ParsingResult.ValidNoUser(Type.STEAM)
                    } else {
                        ParsingResult.ExtractedUser(Type.STEAM, user)
                    }
                }
            }

            // Email
            else -> extractUserFromEmailRecoveryToken(token)
        }
    }

    private fun extractUserFromEmailRecoveryToken(emailRecoveryToken: String): ParsingResult {
        val passwordReset = try {
            fafTokenService.getToken(FafToken.PasswordReset::class, emailRecoveryToken)
        } catch (exception: Exception) {
            metricHelper.incrementPasswordResetViaEmailFailedCounter()
            LOG.error("Unable to extract claims", exception)
            return ParsingResult.Invalid(InvalidRecoveryException("Unable to extract claims from token"))
        }

        val user = userRepository.findById(passwordReset.userId)

        if (user == null) {
            metricHelper.incrementPasswordResetViaEmailFailedCounter()
            return ParsingResult.Invalid(InvalidRecoveryException("User with id ${passwordReset.userId} not found"))
        }

        return ParsingResult.ExtractedUser(Type.EMAIL, user)
    }

    fun resetPassword(type: Type, userId: Int, newPassword: String) {
        loginService.resetPassword(userId, newPassword)
        LOG.info("Password for user id {} has been reset", userId)

        when (type) {
            Type.EMAIL -> metricHelper.incrementPasswordResetViaEmailDoneCounter()
            Type.STEAM -> metricHelper.incrementPasswordResetViaSteamDoneCounter()
        }
    }
}
