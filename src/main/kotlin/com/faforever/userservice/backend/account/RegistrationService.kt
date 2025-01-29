package com.faforever.userservice.backend.account

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
import com.faforever.userservice.backend.tos.TosService
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

sealed interface EmailStatusResponse {
    data object EmailBlackListed : EmailStatusResponse
    data object EmailAvailable : EmailStatusResponse
    data class EmailTaken(val existingUsername: String) : EmailStatusResponse
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
    private val tosService: TosService,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(RegistrationService::class.java)
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
    }

    fun register(username: String, email: String) {
        checkUsername(username)

        when (val emailStatus = emailAvailable(email)) {
            is EmailStatusResponse.EmailBlackListed -> throw IllegalStateException("Email provider is blacklisted")
            is EmailStatusResponse.EmailTaken -> onEmailTaken(username, emailStatus.existingUsername, email)
            is EmailStatusResponse.EmailAvailable -> {
                sendActivationEmail(username, email)
                metricHelper.incrementUserRegistrationCounter()
            }
        }
    }

    private fun onEmailTaken(desiredUsername: String, existingUsername: String, email: String) {
        val passwordResetUrl =
            fafProperties.account().passwordReset().passwordResetInitiateEmailUrlFormat().format(email)
        emailService.sendEmailAlreadyTakenMail(desiredUsername, existingUsername, email, passwordResetUrl)
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
    fun emailAvailable(email: String): EmailStatusResponse {
        val onBlacklist = domainBlacklistRepository.existsByDomain(email.substring(email.lastIndexOf('@') + 1))
        if (onBlacklist) {
            return EmailStatusResponse.EmailBlackListed
        }

        val user = userRepository.findByEmail(email)
        return if (user != null) EmailStatusResponse.EmailTaken(user.username) else EmailStatusResponse.EmailAvailable
    }

    fun validateRegistrationToken(registrationToken: String): RegisteredUser {
        val claims = try {
            fafTokenService.getTokenClaims(FafTokenType.REGISTRATION, registrationToken)
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

        checkUsername(username)
        val emailStatus = emailAvailable(email)
        require(emailStatus is EmailStatusResponse.EmailAvailable) { "Email unavailable" }

        val user = User(
            username = username,
            password = encodedPassword,
            email = email,
            ip = ipAddress.value,
            acceptedTos = tosService.findLatestTos()?.version,
        )

        userRepository.persist(user)

        LOG.info("User has been activated: {}", user)
        metricHelper.incrementUserActivationCounter()

        emailService.sendWelcomeToFafMail(username, email)

        return user
    }

    private fun checkUsername(username: String) {
        val usernameStatus = usernameAvailable(username)
        require(usernameStatus == UsernameStatus.USERNAME_AVAILABLE) { "Username unavailable" }
    }
}
