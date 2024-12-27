package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.metrics.MetricHelper
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.security.FafTokenType
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
        private const val KEY_USER_ID = "id"
    }

    fun requestPasswordResetViaEmail(usernameOrEmail: String) {
        metricHelper.incrementPasswordResetViaEmailRequestCounter()
        val user = userRepository.findByUsernameOrEmail(usernameOrEmail)

        if (user == null) {
            metricHelper.incrementPasswordResetViaEmailFailedCounter()
            LOG.info("No user found for recovery with username/email: {}", usernameOrEmail)
        } else {
            val token = fafTokenService.createToken(
                fafTokenType = FafTokenType.PASSWORD_RESET,
                lifetime = Duration.ofSeconds(fafProperties.account().passwordReset().linkExpirationSeconds()),
                attributes = mapOf(KEY_USER_ID to user.id.toString()),
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

    fun parseRecoveryHttpRequest(parameters: Map<String, List<String>>): Pair<Type, User?> {
        // At first glance it may seem strange that a service is parsing http request parameters,
        // but the parameters of the request are determined by this service itself in the request reset phase!
        val token = parameters["token"]?.first()
        LOG.debug("Extracted token: {}", token)

        val steamId = steamService.parseSteamIdFromRequestParameters(parameters)
        LOG.debug("Extracted Steam id: {}", steamId)

        return when {
            steamId != null -> Type.STEAM to steamService.findUserBySteamId(steamId).also { user ->
                if (user == null) metricHelper.incrementPasswordResetViaSteamFailedCounter()
            }

            token != null -> Type.EMAIL to extractUserFromEmailRecoveryToken(token)
            else -> {
                metricHelper.incrementPasswordResetViaEmailFailedCounter()
                throw InvalidRecoveryException("Could not extract recovery type or user from HTTP request")
            }
        }
    }

    private fun extractUserFromEmailRecoveryToken(emailRecoveryToken: String): User {
        val claims = try {
            fafTokenService.getTokenClaims(FafTokenType.PASSWORD_RESET, emailRecoveryToken)
        } catch (exception: Exception) {
            metricHelper.incrementPasswordResetViaEmailFailedCounter()
            LOG.error("Unable to extract claims", exception)
            throw InvalidRecoveryException("Unable to extract claims from token")
        }

        val userId = claims[KEY_USER_ID]

        if (userId.isNullOrBlank()) {
            metricHelper.incrementPasswordResetViaEmailFailedCounter()
            throw InvalidRecoveryException("No user id found in token claims")
        }

        val user = userRepository.findById(userId.toInt())

        if (user == null) {
            metricHelper.incrementPasswordResetViaEmailFailedCounter()
            throw InvalidRecoveryException("User with id $userId not found")
        }

        return user
    }

    fun resetPassword(type: Type, userId: Int, newPassword: String) {
        loginService.resetPassword(userId, newPassword)

        when (type) {
            Type.EMAIL -> metricHelper.incrementPasswordResetViaEmailDoneCounter()
            Type.STEAM -> metricHelper.incrementPasswordResetViaSteamDoneCounter()
        }
    }
}
