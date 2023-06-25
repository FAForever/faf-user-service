package com.faforever.domain

import io.smallrye.config.ConfigMapping
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.constraints.NotNull
import java.net.URI
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

@JvmInline
value class RedirectTo(val url: String) {
    val uri get() = URI.create(url)
}

sealed interface LoginResult {
    object LoginThrottlingActive : LoginResult
    object UserOrCredentialsMismatch : LoginResult
    object TechnicalError : LoginResult
    data class SuccessfulLogin(val redirectTo: RedirectTo) : LoginResult
    data class UserBanned(val reason: String, val expiresAt: OffsetDateTime?) : LoginResult
    data class UserNoGameOwnership(val redirectTo: RedirectTo) : LoginResult
}

interface UserService {
    fun findUserBySubject(subject: String): User?

    fun login(
        challenge: String,
        usernameOrEmail: String,
        password: String,
        ip: String,
    ): LoginResult

    fun decideConsent(challenge: String, permitted: Boolean): RedirectTo
}

@ApplicationScoped
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val accountLinkRepository: AccountLinkRepository,
): UserService {
    override fun findUserBySubject(subject: String) = userRepository.findByUsernameOrEmail(subject)

    override fun login(challenge: String, usernameOrEmail: String, password: String, ip: String): LoginResult {
        val user = userRepository.findByUsernameOrEmail(usernameOrEmail)

        return if(user != null) {
            LoginResult.SuccessfulLogin(RedirectTo("back"))
        } else {
            LoginResult.UserOrCredentialsMismatch
        }
    }

    override fun decideConsent(challenge: String, permitted: Boolean): RedirectTo {
        return RedirectTo("consentDecided")
    }

}