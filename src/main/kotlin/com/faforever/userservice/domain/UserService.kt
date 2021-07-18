package com.faforever.userservice.domain

import com.faforever.userservice.hydra.HydraService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import sh.ory.hydra.model.AcceptConsentRequest
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.ConsentRequestSession
import sh.ory.hydra.model.GenericError
import sh.ory.hydra.model.LoginRequest
import java.time.LocalDateTime

@ConfigurationProperties(prefix = "security")
@Validated
@ConstructorBinding
data class SecurityProperties(
    val failedLoginAccountThreshold: Int,
    val failedLoginAttemptThreshold: Int,
    val failedLoginThrottlingMinutes: Long,
)

sealed class LoginResult {
    data class LoginThrottlingActive(val redirectTo: String) : LoginResult()
    object UserOrCredentialsMismatch : LoginResult()
    data class SuccessfulLogin(val redirectTo: String) : LoginResult()
    data class UserBanned(val redirectTo: String, val ban: Ban) : LoginResult()
}

@Component
class UserService(
    private val securityProperties: SecurityProperties,
    private val userRepository: UserRepository,
    private val loginLogRepository: LoginLogRepository,
    private val banRepository: BanRepository,
    private val hydraService: HydraService,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(UserService::class.java)
        private const val HYDRA_ERROR_USER_BANNED = "user_banned"
        private const val HYDRA_ERROR_LOGIN_THROTTLED = "login_throttled"

        /**
         * The user role is used to distinguish users from technical accounts.
         */
        private const val ROLE_USER = "USER"
    }

    fun findUserBySubject(subject: String) =
        userRepository.findById(subject.toLong())

    private fun checkLoginThrottlingRequired(ip: String) = loginLogRepository.findFailedAttemptsByIp(ip)
        .map {
            val accountsAffected = it.accountsAffected ?: 0
            val totalFailedAttempts = it.totalAttempts ?: 0

            log.debug("Failed login attempts for IP address '$ip': $it")

            if (accountsAffected > securityProperties.failedLoginAccountThreshold ||
                totalFailedAttempts > securityProperties.failedLoginAttemptThreshold
            ) {
                val lastAttempt = it.lastAttemptAt!!
                if (LocalDateTime.now().minusMinutes(securityProperties.failedLoginThrottlingMinutes)
                    .isBefore(lastAttempt)
                ) {
                    log.debug("IP '$ip' is trying again to early -> throttle it")
                    true
                } else {
                    log.debug("IP '$ip' triggered a threshold but last login does not hit throttling time")
                    false
                }
            } else {
                log.trace("IP '$ip' did not hit a throttling limit")
                false
            }
        }

    fun login(
        challenge: String,
        usernameOrEmail: String,
        password: String,
        ip: String,
    ): Mono<LoginResult> = checkLoginThrottlingRequired(ip)
        .flatMap { throttlingRequired ->
            if (throttlingRequired) {
                hydraService.rejectLoginRequest(challenge, GenericError(HYDRA_ERROR_LOGIN_THROTTLED))
                    .map {
                        LoginResult.LoginThrottlingActive(
                            UriComponentsBuilder.fromUriString("/throttle")
                                .queryParam("login_challenge", challenge)
                                .build()
                                .toUriString()
                        )
                    }
            } else {
                hydraService.getLoginRequest(challenge)
                    .flatMap { loginRequest ->
                        internalLogin(challenge, usernameOrEmail, password, ip, loginRequest)
                    }
            }
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
        loginRequest: LoginRequest,
    ): Mono<LoginResult> = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
        .flatMap { user ->
            if (loginRequest.skip || passwordEncoder.matches(password, user.password)) {
                logLoginAttempt(user, ip, true)
                    .flatMap { findActiveGlobalBan(user) }
                    .flatMap<LoginResult> { ban ->
                        log.debug("User '$usernameOrEmail' is banned by $ban")
                        hydraService.rejectLoginRequest(challenge, GenericError(HYDRA_ERROR_USER_BANNED))
                            .map { LoginResult.UserBanned(it.redirectTo, ban) }
                    }
                    .switchIfEmpty {
                        log.debug("User '$usernameOrEmail' logged in successfully")

                        hydraService.acceptLoginRequest(
                            challenge,
                            AcceptLoginRequest(user.id.toString())
                        ).map { LoginResult.SuccessfulLogin(it.redirectTo) }
                    }
            } else {
                log.debug("Password for user '$usernameOrEmail' doesn't match")
                logLoginAttempt(user, ip, false)
                    .map { LoginResult.UserOrCredentialsMismatch }
            }
        }
        .switchIfEmpty {
            log.debug("User '$usernameOrEmail' not found")
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
                    userRepository.findUserPermissions(consentRequest.subject?.toInt() ?: -1)
                        .collectList()
                        .flatMap { permissions ->
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
                                        accessToken = mapOf("roles" to roles),
                                        idToken = mapOf("roles" to roles)
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
