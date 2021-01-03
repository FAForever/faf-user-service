package com.faforever.userservice.domain

import com.faforever.userservice.hydra.HydraService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import sh.ory.hydra.model.AcceptLoginRequest
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
    val securityProperties: SecurityProperties,
    val userRepository: UserRepository,
    val loginLogRepository: LoginLogRepository,
    val banRepository: BanRepository,
    val hydraService: HydraService,
    val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(UserService::class.java)
        private const val HYDRA_ERROR_USER_BANNED = "user_banned"
        private const val HYDRA_ERROR_LOGIN_THROTTLED = "login_throttled"
    }

    private fun checkLoginThrottlingRequired(ip: String) = loginLogRepository.findFailedAttemptsByIp(ip)
        .map {
            val accountsAffected = it.accountsAffected ?: 0
            val totalFailedAttempts = it.totalAttempts ?: 0

            log.debug("Failed login attempts for IP address '$ip': $it")

            if (accountsAffected > securityProperties.failedLoginAccountThreshold ||
                totalFailedAttempts > securityProperties.failedLoginAttemptThreshold
            ) {
                val lastAttempt = it.lastAttemptAt!!
                if (LocalDateTime.now().minusMinutes(securityProperties.failedLoginThrottlingMinutes).isBefore(lastAttempt)) {
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
        username: String,
        password: String,
        ip: String,
    ): Mono<LoginResult> = checkLoginThrottlingRequired(ip)
        .flatMap { throttlingRequired ->
            if (throttlingRequired) {
                hydraService.rejectLoginRequest(challenge, GenericError(HYDRA_ERROR_LOGIN_THROTTLED))
                    .map { LoginResult.LoginThrottlingActive(it.redirectTo) }
            } else {
                hydraService.getLoginRequest(challenge)
                    .flatMap { loginRequest ->
                        internalLogin(challenge, username, password, ip, loginRequest)
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
        username: String,
        password: String,
        ip: String,
        loginRequest: LoginRequest,
    ): Mono<LoginResult> = userRepository.findByUsername(username)
        .flatMap { user ->
            if (loginRequest.skip || passwordEncoder.matches(password, user.password)) {
                updateLoginAttempts(user, ip, true)
                    .flatMap { findActiveGlobalBan(user) }
                    .flatMap<LoginResult> { ban ->
                        log.debug("User '$username' is banned by $ban")
                        hydraService.rejectLoginRequest(challenge, GenericError(HYDRA_ERROR_USER_BANNED))
                            .map { LoginResult.UserBanned(it.redirectTo, ban) }
                    }
                    .switchIfEmpty {
                        log.debug("User '$username' logged in successfully")

                        hydraService.acceptLoginRequest(
                            challenge,
                            AcceptLoginRequest(user.id.toString())
                        ).map { LoginResult.SuccessfulLogin(it.redirectTo) }
                    }
            } else {
                log.debug("Password for user '$username' doesn't match")
                updateLoginAttempts(user, ip, false)
                    .map { LoginResult.UserOrCredentialsMismatch }
            }
        }
        .switchIfEmpty {
            log.debug("User '$username' not found")
            // TODO: update failed login attempts (the current database scheme requires an account id - doesn't work)
            Mono.just(LoginResult.UserOrCredentialsMismatch)
        }

    private fun findActiveGlobalBan(user: User): Mono<Ban> =
        banRepository.findAllByPlayerIdAndLevel(user.id, BanLevel.GLOBAL)
            .filter { it.isActive }
            .next()

    private fun updateLoginAttempts(user: User, ip: String, success: Boolean) =
        loginLogRepository.findByUserIdAndIpAndSuccess(user.id, ip, success)
            .flatMap {
                log.trace("IP address already had ${if (success) "successful" else "failed"} login attempts on this user, increment attempts")
                loginLogRepository.save(it.copy(attempts = it.attempts + 1, updateTime = LocalDateTime.now()))
            }
            .switchIfEmpty {
                log.trace("IP address ${if (success) "successful" else "failed"} login on this user for the first time")
                loginLogRepository.save(LoginLog(0, user.id, ip, 1, success))
            }
}
