package com.faforever.userservice.backend.domain

import com.faforever.domain.Ban
import com.faforever.domain.BanRepository
import com.faforever.userservice.backend.security.PasswordEncoder
import io.smallrye.config.ConfigMapping
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.constraints.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime

@ConfigMapping(prefix = "security")
interface SecurityProperties {
    @NotNull
    fun failedLoginAccountThreshold(): Int

    @NotNull
    fun failedLoginAttemptThreshold(): Int

    @NotNull
    fun failedLoginThrottlingMinutes(): Long

    @NotNull
    fun failedLoginDaysToCheck(): Long
}


/**
 * This interface makes sure we consistently return the same set of user information
 */
private interface LoginUserInfo {
    val userId: Long
    val userName: String
}

sealed interface LoginResult {
    object LoginThrottlingActive : LoginResult
    object UserOrCredentialsMismatch : LoginResult
    object TechnicalError : LoginResult
    data class SuccessfulLogin(
        override val userId: Long,
        override val userName: String,
    ) : LoginResult, LoginUserInfo

    data class UserBanned(
        override val userId: Long,
        override val userName: String,
        val reason: String,
        val expiresAt: OffsetDateTime?,
    ) : LoginResult, LoginUserInfo

    data class UserNoGameOwnership(
        override val userId: Long,
        override val userName: String,
    ) : LoginResult, LoginUserInfo
}

interface LoginService {
    fun findUserBySubject(subject: String): User?

    fun login(usernameOrEmail: String, password: String, ip: IpAddress, requiresGameOwnership: Boolean): LoginResult
}

@ApplicationScoped
class LoginServiceImpl(
    private val securityProperties: SecurityProperties,
    private val userRepository: UserRepository,
    private val loginLogRepository: LoginLogRepository,
    private val accountLinkRepository: AccountLinkRepository,
    private val passwordEncoder: PasswordEncoder,
    private val banRepository: BanRepository,
) : LoginService {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(LoginServiceImpl::class.java)
    }

    override fun findUserBySubject(subject: String) = userRepository.findByUsernameOrEmail(subject)

    override fun login(usernameOrEmail: String, password: String, ip: IpAddress, requiresGameOwnership: Boolean): LoginResult {
        val user = userRepository.findByUsernameOrEmail(usernameOrEmail)

        if (user == null || !passwordEncoder.matches(password, user.password)) {
            logFailedLoginAttempt(usernameOrEmail, ip)
            return LoginResult.UserOrCredentialsMismatch
        }

        if (throttlingRequired(ip)) {
            logLoginAttempt(user, ip, false)
            return LoginResult.LoginThrottlingActive
        }

        logLoginAttempt(user, ip, true)

        val activeGlobalBan = findActiveGlobalBan(user)

        if (activeGlobalBan != null) {
            LOG.debug("User '{}' is banned by {}", usernameOrEmail, activeGlobalBan)
            return LoginResult.UserBanned(user.id, user.username, activeGlobalBan.reason, activeGlobalBan.expiresAt)
        }

        if (requiresGameOwnership && !accountLinkRepository.hasOwnershipLink(user.id)) {
            LOG.debug("Lobby login blocked for user '{}' because of missing game ownership verification", usernameOrEmail)
            return LoginResult.UserNoGameOwnership(user.id, user.username)
        }

        LOG.debug("User '{}' logged in successfully", usernameOrEmail)
        return LoginResult.SuccessfulLogin(user.id, user.username)
    }

    private fun logLoginAttempt(user: User, ip: IpAddress, success: Boolean) =
        loginLogRepository.persist(LoginLog(0, user.id, null, ip.value, success))

    private fun logFailedLoginAttempt(unknownLogin: String, ip: IpAddress) =
        loginLogRepository.persist(LoginLog(0, null, unknownLogin.take(100), ip.value, false))

    private fun findActiveGlobalBan(user: User): Ban? =
        banRepository.findGlobalBansByPlayerId(user.id)
            .firstOrNull { it.isActive }

    private fun throttlingRequired(ip: IpAddress): Boolean {
        val failedAttemptsSummary = loginLogRepository.findFailedAttemptsByIpAfterDate(
            ip.value,
            LocalDateTime.now().minusDays(securityProperties.failedLoginDaysToCheck()),
        ) ?: FailedAttemptsSummary(0, 0, null, null)

        val accountsAffected = failedAttemptsSummary.accountsAffected
        val totalFailedAttempts = failedAttemptsSummary.totalAttempts

        LOG.debug("Failed login attempts for IP address '{}': {}", ip, failedAttemptsSummary)

        return if (accountsAffected > securityProperties.failedLoginAccountThreshold() ||
            totalFailedAttempts > securityProperties.failedLoginAttemptThreshold()
        ) {
            val lastAttempt = failedAttemptsSummary.lastAttemptAt!!
            if (LocalDateTime.now()
                    .minusMinutes(securityProperties.failedLoginThrottlingMinutes())
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
}