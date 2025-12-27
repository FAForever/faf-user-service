package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.AccountLinkRepository
import com.faforever.userservice.backend.domain.Ban
import com.faforever.userservice.backend.domain.BanRepository
import com.faforever.userservice.backend.domain.FailedAttemptsSummary
import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.LoginLog
import com.faforever.userservice.backend.domain.LoginLogRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.security.PasswordEncoder
import io.smallrye.config.ConfigMapping
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.validation.constraints.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

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

sealed interface LoginResult {
    sealed interface RecoverableLoginFailure : LoginResult
    object ThrottlingActive : RecoverableLoginFailure
    object RecoverableLoginOrCredentialsMismatch : RecoverableLoginFailure
    data class MissedBan(
        val reason: String,
        val startTime: OffsetDateTime,
        val endTime: OffsetDateTime,
    ) : RecoverableLoginFailure

    sealed interface UnrecoverableLoginFailure : LoginResult
    object TechnicalError : UnrecoverableLoginFailure
    object UserNoGameOwnership : UnrecoverableLoginFailure
    data class UserBanned(
        val reason: String,
        val expiresAt: OffsetDateTime?,
    ) : UnrecoverableLoginFailure

    data class SuccessfulLogin(
        val userId: Int,
        val userName: String,
    ) : LoginResult
}

interface LoginService {
    fun findUserBySubject(subject: String): User?

    fun login(usernameOrEmail: String, password: String, ip: IpAddress, requiresGameOwnership: Boolean): LoginResult

    fun resetPassword(userId: Int, newPassword: String)
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

    override fun login(usernameOrEmail: String, password: String, ip: IpAddress, requiresGameOwnership: Boolean):
        LoginResult {
        if (throttlingRequired(ip)) {
            return LoginResult.ThrottlingActive
        }

        val user = userRepository.findByUsernameOrEmail(usernameOrEmail)
        if (user == null || !passwordEncoder.matches(password, user.password)) {
            logFailedLogin(usernameOrEmail, ip)
            return LoginResult.RecoverableLoginOrCredentialsMismatch
        }

        val lastLogin = loginLogRepository.findLastLoginTime(user.id!!)
            ?.atZone(ZoneId.systemDefault())?.toOffsetDateTime()
        logLogin(usernameOrEmail, user, ip)

        val activeGlobalBan = findActiveGlobalBan(user)
        if (activeGlobalBan != null) {
            LOG.debug("User '{}' is banned by {}", usernameOrEmail, activeGlobalBan)
            return LoginResult.UserBanned(activeGlobalBan.reason, activeGlobalBan.expiresAt)
        }

        val missedGlobalBan = findMissedGlobalBan(user, lastLogin ?: OffsetDateTime.now().minusDays(90))
        if (missedGlobalBan != null) {
            LOG.debug(
                "User '{}' missed a ban {} and needs to be informed about it. Login blocked.",
                usernameOrEmail,
                missedGlobalBan,
            )
            return LoginResult.MissedBan(
                missedGlobalBan.reason,
                missedGlobalBan.createTime,
                missedGlobalBan.expiresAt!!,
            )
        }

        if (requiresGameOwnership && !accountLinkRepository.hasOwnershipLink(user.id!!)) {
            LOG.debug(
                "Lobby login blocked for user '{}' because of missing game ownership verification",
                usernameOrEmail,
            )
            return LoginResult.UserNoGameOwnership
        }

        LOG.debug("User '{}' logged in successfully", usernameOrEmail)
        return LoginResult.SuccessfulLogin(user.id!!, user.username)
    }

    private fun logLogin(loginString: String, user: User, ip: IpAddress) =
        loginLogRepository.persist(LoginLog(0, user.id, loginString, ip.value, true))

    private fun logFailedLogin(unknownLogin: String, ip: IpAddress) =
        loginLogRepository.persist(LoginLog(0, null, unknownLogin.take(100), ip.value, false))

    private fun findActiveGlobalBan(user: User): Ban? =
        banRepository.findGlobalBansByPlayerId(user.id!!)
            .firstOrNull { it.isActive }

    private fun findMissedGlobalBan(user: User, lastLogin: OffsetDateTime): Ban? {
        return banRepository.findGlobalBansByPlayerId(user.id!!)
            .firstOrNull {
                it.revokeTime == null && it.expiresAt != null && it.createTime.isAfter(lastLogin)
            }
    }

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

    @Transactional
    override fun resetPassword(userId: Int, newPassword: String) {
        userRepository.findById(userId)!!.apply {
            password = passwordEncoder.encode(newPassword)
            userRepository.persist(this)
        }

        // Disabled due to #377 -- revoking consent requests does not seem the right way to disable all active sessions
        // hydraService.revokeConsentRequest(userId.toString())

        LOG.info("Password for user id {} has been reset", userId)
    }
}
