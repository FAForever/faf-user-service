package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.AccountRequest
import com.faforever.userservice.backend.domain.AccountRequestRepository
import com.faforever.userservice.backend.domain.AccountRequestType
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.security.FafTokenType
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

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
    data object PendingChangeNotFound : EmailChangeConfirmationResult
    data object UserNotFound : EmailChangeConfirmationResult
    data object EmailUnavailable : EmailChangeConfirmationResult
}

@ApplicationScoped
class EmailChangeService(
    private val userRepository: UserRepository,
    private val accountRequestRepository: AccountRequestRepository,
    private val emailService: EmailService,
    private val fafTokenService: FafTokenService,
    private val fafProperties: FafProperties,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(EmailChangeService::class.java)
        private const val KEY_CHANGE_ID = "changeId"
        private const val KEY_USER_ID = "userId"
        private const val KEY_NEW_EMAIL = "newEmail"
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
        val changeId = UUID.randomUUID().toString()
        val lifetime = Duration.ofSeconds(fafProperties.account().emailChange().linkExpirationSeconds())
        val expiresAt = OffsetDateTime.now().plus(lifetime)
        val token = fafTokenService.createToken(
            FafTokenType.EMAIL_CHANGE,
            lifetime,
            mapOf(
                KEY_CHANGE_ID to changeId,
                KEY_USER_ID to userId.toString(),
                KEY_NEW_EMAIL to newEmail,
            ),
        )

        accountRequestRepository.deleteByUserIdAndType(userId, AccountRequestType.EMAIL_CHANGE)
        accountRequestRepository.persist(
            AccountRequest(
                id = changeId,
                userId = userId,
                type = AccountRequestType.EMAIL_CHANGE,
                tokenHash = hashToken(token),
                expiresAt = expiresAt,
                data = mapOf(KEY_NEW_EMAIL to newEmail),
            ),
        )

        val confirmationUrl = fafProperties.account().emailChange().confirmationUrlFormat().format(token)
        emailService.sendEmailChangeConfirmationMail(user.username, newEmail, confirmationUrl)
    }

    @Transactional
    fun confirmEmailChange(token: String): EmailChangeConfirmationResult {
        val claims = try {
            fafTokenService.getTokenClaims(FafTokenType.EMAIL_CHANGE, token)
        } catch (exception: Exception) {
            LOG.info("Unable to extract email change token claims", exception)
            return EmailChangeConfirmationResult.InvalidToken
        }

        val changeId = claims[KEY_CHANGE_ID]
        val userId = claims[KEY_USER_ID]?.toIntOrNull()
        val newEmail = claims[KEY_NEW_EMAIL]
        if (changeId.isNullOrBlank() || userId == null || newEmail.isNullOrBlank()) {
            return EmailChangeConfirmationResult.InvalidToken
        }

        val pendingChange = accountRequestRepository.findById(changeId)
            ?: return EmailChangeConfirmationResult.PendingChangeNotFound
        val pendingNewEmail = pendingChange.data[KEY_NEW_EMAIL]
        if (pendingChange.userId != userId ||
            pendingChange.type != AccountRequestType.EMAIL_CHANGE ||
            pendingNewEmail != newEmail ||
            pendingChange.tokenHash != hashToken(token)
        ) {
            return EmailChangeConfirmationResult.InvalidToken
        }

        val user = userRepository.findById(userId)
            ?: return EmailChangeConfirmationResult.UserNotFound.also {
                accountRequestRepository.delete(pendingChange)
            }

        return when (emailAvailabilityForUser(newEmail, user)) {
            EmailAvailability.AVAILABLE -> {
                val previousEmail = user.email
                user.email = newEmail
                accountRequestRepository.delete(pendingChange)
                emailService.sendEmailChangedNotificationMail(user.username, previousEmail, newEmail)
                EmailChangeConfirmationResult.Confirmed
            }
            EmailAvailability.UNCHANGED -> {
                accountRequestRepository.delete(pendingChange)
                EmailChangeConfirmationResult.Confirmed
            }
            EmailAvailability.INVALID,
            EmailAvailability.BLACKLISTED,
            EmailAvailability.TAKEN,
            -> {
                accountRequestRepository.delete(pendingChange)
                EmailChangeConfirmationResult.EmailUnavailable
            }
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

    private fun hashToken(token: String): String =
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))

    private enum class EmailAvailability {
        AVAILABLE,
        INVALID,
        BLACKLISTED,
        TAKEN,
        UNCHANGED,
    }
}
