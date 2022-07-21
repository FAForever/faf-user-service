package com.faforever.userservice.domain

import com.faforever.userservice.hydra.HydraService
import com.faforever.userservice.security.OAuthScope
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Context
import io.micronaut.http.uri.UriBuilder
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2
import sh.ory.hydra.model.AcceptConsentRequest
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.ConsentRequestSession
import sh.ory.hydra.model.GenericError
import sh.ory.hydra.model.LoginRequest
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.validation.constraints.NotNull

@ConfigurationProperties("security")
@Context
interface SecurityProperties {
    @get:NotNull
    val failedLoginAccountThreshold: Int

    @get:NotNull
    val failedLoginAttemptThreshold: Int

    @get:NotNull
    val failedLoginThrottlingMinutes: Long

    @get:NotNull
    val failedLoginDaysToCheck: Long
}

sealed class LoginResult {
    object LoginThrottlingActive : LoginResult()
    object UserOrCredentialsMismatch : LoginResult()
    object TechnicalError : LoginResult()
    data class SuccessfulLogin(val redirectTo: String) : LoginResult()
    data class UserBanned(val reason: String, val expiresAt: OffsetDateTime?) : LoginResult()
    data class UserNoGameOwnership(val redirectTo: String) : LoginResult()
}

@Singleton
class UserService(
    private val securityProperties: SecurityProperties,
    private val userRepository: UserRepository,
    private val accountLinkRepository: AccountLinkRepository,
    private val loginLogRepository: LoginLogRepository,
    private val banRepository: BanRepository,
    private val hydraService: HydraService,
    private val passwordEncoder: PasswordEncoder
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(UserService::class.java)
        private const val HYDRA_ERROR_USER_BANNED = "user_banned"
        private const val HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION = "ownership_not_verified"

        /**
         * The user role is used to distinguish users from technical accounts.
         */
        private const val ROLE_USER = "USER"
    }

    fun findUserBySubject(subject: String) =
        userRepository.findById(subject.toInt())

    private fun checkLoginThrottlingRequired(ip: String) = loginLogRepository.findFailedAttemptsByIpAfterDate(
        ip,
        LocalDateTime.now().minusDays(securityProperties.failedLoginDaysToCheck)
    )
        .map {
            val accountsAffected = it.accountsAffected ?: 0
            val totalFailedAttempts = it.totalAttempts ?: 0

            LOG.debug("Failed login attempts for IP address '$ip': $it")

            if (accountsAffected > securityProperties.failedLoginAccountThreshold ||
                totalFailedAttempts > securityProperties.failedLoginAttemptThreshold
            ) {
                val lastAttempt = it.lastAttemptAt!!
                if (LocalDateTime.now().minusMinutes(securityProperties.failedLoginThrottlingMinutes)
                        .isBefore(lastAttempt)
                ) {
                    LOG.debug("IP '$ip' is trying again to early -> throttle it")
                    true
                } else {
                    LOG.debug("IP '$ip' triggered a threshold but last login does not hit throttling time")
                    false
                }
            } else {
                LOG.trace("IP '$ip' did not hit a throttling limit")
                false
            }
        }

    fun login(
        challenge: String,
        usernameOrEmail: String,
        password: String,
        ip: String
    ): Mono<LoginResult> = checkLoginThrottlingRequired(ip)
        .flatMap { throttlingRequired ->
            if (throttlingRequired) {
                LoginResult.LoginThrottlingActive.toMono()
            } else {
                hydraService.getLoginRequest(challenge)
                    .flatMap { loginRequest ->
                        internalLogin(challenge, usernameOrEmail, password, ip, loginRequest)
                    }
            }
        }
        .onErrorResume { error ->
            LOG.debug("Login failed with technical error for challenge $challenge", error)
            LoginResult.TechnicalError.toMono()
        }

    /**
     * * Validates the credentials
     * * Updates the login attempts
     * * Checks for bans
     * * Inform Ory Hydra about the result
     */
    private fun internalLogin(
        challenge: String,
        usernameOrEmail: String,
        password: String,
        ip: String,
        loginRequest: LoginRequest
    ): Mono<LoginResult> = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
        .flatMap { user ->
            if (loginRequest.skip || passwordEncoder.matches(password, user.password)) {
                logLoginAttempt(user, ip, true)
                    .flatMap { findActiveGlobalBan(user) }
                    .flatMap<LoginResult> { ban ->
                        LOG.debug("User '$usernameOrEmail' is banned by $ban")
                        hydraService.rejectLoginRequest(challenge, GenericError(HYDRA_ERROR_USER_BANNED))
                            .map {
                                LoginResult.UserBanned(ban.reason, ban.expiresAt)
                            }
                    }
                    .switchIfEmpty {
                        if (loginRequest.requestedScope.contains(OAuthScope.LOBBY)) {
                            accountLinkRepository.existsByUserIdAndOwnership(user.id, true).flatMap {exists ->
                                if (exists) {
                                    LOG.debug("User '$usernameOrEmail' logged in successfully")

                                    hydraService.acceptLoginRequest(
                                        challenge,
                                        AcceptLoginRequest(user.id.toString())
                                    ).map { LoginResult.SuccessfulLogin(it.redirectTo) }
                                } else {
                                    LOG.debug("Lobby login blocked for user '$usernameOrEmail' because of missing game ownership verification")

                                    hydraService.rejectLoginRequest(
                                        challenge,
                                        GenericError(HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION)
                                    ).map {
                                        LoginResult.UserNoGameOwnership(
                                            UriBuilder.of("/oauth2/gameVerificationFailed")
                                                .build()
                                                .toASCIIString()
                                        )
                                    }
                                }
                            }
                        } else {
                            LOG.debug("User '$usernameOrEmail' logged in successfully")

                            hydraService.acceptLoginRequest(
                                challenge,
                                AcceptLoginRequest(user.id.toString())
                            ).map { LoginResult.SuccessfulLogin(it.redirectTo) }
                        }
                    }
            } else {
                LOG.debug("Password for user '$usernameOrEmail' doesn't match")
                logLoginAttempt(user, ip, false)
                    .map { LoginResult.UserOrCredentialsMismatch }
            }
        }
        .switchIfEmpty {
            LOG.debug("User '$usernameOrEmail' not found")
            logFailedLoginAttempt(usernameOrEmail, ip).map {
                LoginResult.UserOrCredentialsMismatch
            }
        }

    private fun findActiveGlobalBan(user: User): Mono<Ban> =
        banRepository.findAllByPlayerIdAndLevel(user.id, BanLevel.GLOBAL)
            .filter { it.isActive }
            .next()

    private fun logLoginAttempt(user: User, ip: String, success: Boolean) =
        loginLogRepository.save(LoginLog(0, user.id, null, ip, success))

    private fun logFailedLoginAttempt(unknownLogin: String, ip: String) =
        loginLogRepository.save(LoginLog(0, null, unknownLogin.take(100), ip, false))

    /**
     * Responds to the consent request based on the user response.
     * Returns a redirect url to Ory Hydra in all cases
     */
    fun decideConsent(challenge: String, permitted: Boolean): Mono<String> =
        hydraService.getConsentRequest(challenge)
            .flatMap { consentRequest ->
                if (permitted) {
                    val userId = consentRequest.subject?.toInt() ?: -1
                    Mono.zip(
                        userRepository.findById(userId),
                        userRepository.findUserPermissions(userId).collectList()
                    ).flatMap { (user, permissions) ->
                        val roles = listOf(ROLE_USER) + permissions.map { it.technicalName }

                        /**
                         * *** Why do we put the FAF roles into the access token? ***
                         *
                         * FAF uses OAuth 2.0 / OpenID Connect as a SSO solution. To avoid looking up the
                         * permissions in each service, we put them into the access token right away.
                         *
                         * If you are an external developer and need to utilize some sort of permission system then
                         * you should NOT rely on FAF roles! You have no guarantee that a role still exists tomorrow
                         * and you also have no influence on which user has which role.
                         */
                        hydraService.acceptConsentRequest(
                            challenge,
                            AcceptConsentRequest(
                                session = ConsentRequestSession(
                                    accessToken = mapOf("username" to user.username, "roles" to roles),
                                    idToken = mapOf("username" to user.username, "roles" to roles)
                                ),
                                grantScope = consentRequest.requestedScope
                            )
                        )
                    }
                } else {
                    hydraService.rejectConsentRequest(challenge, GenericError("scope_denied"))
                }
            }.map {
                it.redirectTo
            }
}
