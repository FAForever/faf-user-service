package com.faforever.userservice.backend.registration

import com.faforever.userservice.backend.domain.DomainBlacklistRepository
import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.NameRecordRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.metrics.MetricHelper
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.security.FafTokenType
import com.faforever.userservice.backend.security.PasswordEncoder
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

enum class UsernameStatus {
    USERNAME_TAKEN, USERNAME_RESERVED, USERNAME_AVAILABLE,
}

enum class EmailStatus {
    EMAIL_TAKEN, EMAIL_BLACKLISTED, EMAIL_AVAILABLE,
}

data class RegisteredUser(
    val username: String,
    val email: String,
)

@ApplicationScoped
class RegistrationService(
    private val userRepository: UserRepository,
    private val nameRecordRepository: NameRecordRepository,
    private val domainBlacklistRepository: DomainBlacklistRepository,
    private val passwordEncoder: PasswordEncoder,
    private val fafTokenService: FafTokenService,
    private val fafProperties: FafProperties,
    private val emailService: EmailService,
    private val metricHelper: MetricHelper,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(RegistrationService::class.java)
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_USER_ID = "id"
    }

    fun register(username: String, email: String) {
        checkUsernameAndEmail(username, email)

        sendActivationEmail(username, email)
        metricHelper.userRegistrationCounter.increment()
    }

    private fun sendActivationEmail(username: String, email: String) {
        val token = fafTokenService.createToken(
            FafTokenType.REGISTRATION,
            Duration.ofSeconds(fafProperties.account().registration().linkExpirationSeconds()),
            mapOf(
                KEY_USERNAME to username,
                KEY_EMAIL to email,
            ),
        )
        val activationUrl = fafProperties.account().registration().activationUrlFormat().format(token)
        emailService.sendActivationMail(username, email, activationUrl)
    }

    fun resetPassword(user: User) {
        sendPasswordResetEmail(user)
        metricHelper.userPasswordResetRequestCounter.increment()
    }

    private fun sendPasswordResetEmail(user: User) {
        val token = fafTokenService.createToken(
            FafTokenType.REGISTRATION,
            Duration.ofSeconds(fafProperties.account().passwordReset().linkExpirationSeconds()),
            mapOf(
                KEY_USER_ID to user.id.toString(),
            ),
        )
        val passwordResetUrl = fafProperties.account().passwordReset().passwordResetUrlFormat().format(token)
        emailService.sendPasswordResetMail(user.username, user.email, passwordResetUrl)
    }

    @Transactional
    fun usernameAvailable(username: String): UsernameStatus {
        val exists = userRepository.existsByUsername(username)
        if (exists) {
            return UsernameStatus.USERNAME_TAKEN
        }

        val reserved = nameRecordRepository.existsByPreviousNameAndChangeTimeAfter(
            username,
            OffsetDateTime.now().minusMonths(fafProperties.account().username().usernameReservationTimeInMonths()),
        )

        return if (reserved) UsernameStatus.USERNAME_RESERVED else UsernameStatus.USERNAME_AVAILABLE
    }

    @Transactional
    fun emailAvailable(email: String): EmailStatus {
        val onBlacklist = domainBlacklistRepository.existsByDomain(email.substring(email.lastIndexOf('@') + 1))
        if (onBlacklist) {
            return EmailStatus.EMAIL_BLACKLISTED
        }

        val exists = userRepository.existsByEmail(email)
        return if (exists) EmailStatus.EMAIL_TAKEN else EmailStatus.EMAIL_AVAILABLE
    }

    fun validateRegistrationToken(registrationToken: String): RegisteredUser {
        val claims: Map<String, String>
        try {
            claims = fafTokenService.getTokenClaims(FafTokenType.REGISTRATION, registrationToken)
        } catch (exception: Exception) {
            LOG.error("Unable to extract claims", exception)
            throw InvalidRegistrationException()
        }

        if (claims[KEY_USERNAME].isNullOrBlank() || claims[KEY_EMAIL].isNullOrBlank()) {
            throw InvalidRegistrationException()
        }

        return RegisteredUser(claims[KEY_USERNAME]!!, claims[KEY_EMAIL]!!)
    }

    @Transactional
    fun activate(registeredUser: RegisteredUser, ipAddress: IpAddress, password: String): User {
        val username = registeredUser.username
        val email = registeredUser.email
        val encodedPassword = passwordEncoder.encode(password)

        checkUsernameAndEmail(username, email)

        val user = User(
            username = username,
            password = encodedPassword,
            email = email,
            ip = ipAddress.value,
        )

        userRepository.persist(user)

        LOG.info("User has been activated: {}", user)
        metricHelper.userActivationCounter.increment()

        emailService.sendWelcomeToFafMail(username, email)

        return user
    }

    private fun checkUsernameAndEmail(username: String, email: String) {
        val usernameStatus = usernameAvailable(username)
        if (usernameStatus != UsernameStatus.USERNAME_AVAILABLE) {
            throw IllegalArgumentException("Username unavailable")
        }

        val emailStatus = emailAvailable(email)
        if (emailStatus != EmailStatus.EMAIL_AVAILABLE) {
            throw IllegalArgumentException("Email unavailable")
        }
    }
}
