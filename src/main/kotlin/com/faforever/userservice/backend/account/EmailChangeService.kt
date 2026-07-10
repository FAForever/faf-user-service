package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.FafToken
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

sealed interface EmailChangeRequestResult {
    data object ConfirmationSent : EmailChangeRequestResult
    data object UserNotFound : EmailChangeRequestResult
    data object InvalidEmail : EmailChangeRequestResult
    data object BlacklistedEmail : EmailChangeRequestResult
    data object EmailAlreadyTaken : EmailChangeRequestResult
    data object UnchangedEmail : EmailChangeRequestResult
}

sealed interface EmailChangeConfirmationResult {
    data object Confirmed : EmailChangeConfirmationResult
    data object InvalidToken : EmailChangeConfirmationResult
    data object UserNotFound : EmailChangeConfirmationResult
    data object EmailUnavailable : EmailChangeConfirmationResult
}

@ApplicationScoped
class EmailChangeService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val fafTokenService: FafTokenService,
    private val fafProperties: FafProperties,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(EmailChangeService::class.java)
    }

    @Transactional
    fun requestEmailChange(userId: Int, newEmail: String): EmailChangeRequestResult {
        val normalizedEmail = newEmail.trim().lowercase()
        val user = userRepository.findById(userId) ?: return EmailChangeRequestResult.UserNotFound

        return when (emailAvailabilityForUser(normalizedEmail, user)) {
            EmailAvailability.AVAILABLE -> {
                createEmailChangeRequest(user, normalizedEmail)
                EmailChangeRequestResult.ConfirmationSent
            }
            EmailAvailability.INVALID -> EmailChangeRequestResult.InvalidEmail
            EmailAvailability.BLACKLISTED -> EmailChangeRequestResult.BlacklistedEmail
            EmailAvailability.TAKEN -> EmailChangeRequestResult.EmailAlreadyTaken
            EmailAvailability.UNCHANGED -> EmailChangeRequestResult.UnchangedEmail
        }
    }

    private fun createEmailChangeRequest(user: User, newEmail: String) {
        val userId = user.id ?: error("Cannot change email for a user without an id")
        val lifetime = Duration.ofSeconds(fafProperties.account().emailChange().linkExpirationSeconds())
        val token = fafTokenService.createToken(
            FafToken.EmailChange(userId = userId, newEmail = newEmail),
            lifetime,
        )

        val confirmationUrl = fafProperties.account().emailChange().confirmationUrlFormat().format(token)
        emailService.sendEmailChangeConfirmationMail(user.username, newEmail, confirmationUrl)
    }

    @Transactional
    fun confirmEmailChange(token: String): EmailChangeConfirmationResult {
        val emailChange = try {
            fafTokenService.consumeToken(FafToken.EmailChange::class, token)
        } catch (exception: IllegalArgumentException) {
            LOG.info("Unable to extract email change token claims", exception)
            return EmailChangeConfirmationResult.InvalidToken
        }

        val user = userRepository.findById(emailChange.userId)
            ?: return EmailChangeConfirmationResult.UserNotFound

        return when (emailAvailabilityForUser(emailChange.newEmail, user)) {
            EmailAvailability.AVAILABLE -> {
                val previousEmail = user.email
                user.email = emailChange.newEmail
                emailService.sendEmailChangedNotificationMail(user.username, previousEmail, emailChange.newEmail)
                EmailChangeConfirmationResult.Confirmed
            }
            EmailAvailability.UNCHANGED -> EmailChangeConfirmationResult.Confirmed
            EmailAvailability.INVALID,
            EmailAvailability.BLACKLISTED,
            EmailAvailability.TAKEN,
            -> EmailChangeConfirmationResult.EmailUnavailable
        }
    }

    private fun emailAvailabilityForUser(email: String, user: User): EmailAvailability {
        return when (emailService.validateEmailAddress(email)) {
            EmailService.ValidationResult.INVALID -> EmailAvailability.INVALID
            EmailService.ValidationResult.BLACKLISTED -> EmailAvailability.BLACKLISTED
            EmailService.ValidationResult.VALID -> {
                if (email.equals(user.email, ignoreCase = true)) {
                    EmailAvailability.UNCHANGED
                } else {
                    val existingUser = userRepository.findByEmail(email)
                    if (existingUser != null && existingUser.id != user.id) {
                        EmailAvailability.TAKEN
                    } else {
                        EmailAvailability.AVAILABLE
                    }
                }
            }
        }
    }

    private enum class EmailAvailability {
        AVAILABLE,
        INVALID,
        BLACKLISTED,
        TAKEN,
        UNCHANGED,
    }
}
