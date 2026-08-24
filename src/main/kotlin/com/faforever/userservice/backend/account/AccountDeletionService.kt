package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.FafToken
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.TransactionSynchronizationRegistry
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

sealed interface AccountDeletionRequestResult {
    data object ConfirmationSent : AccountDeletionRequestResult
    data object UserNotFound : AccountDeletionRequestResult
}

sealed interface AccountDeletionConfirmationResult {
    data object Confirmed : AccountDeletionConfirmationResult
    data object InvalidToken : AccountDeletionConfirmationResult
    data object UserNotFound : AccountDeletionConfirmationResult
    data object AnonymizationFailed : AccountDeletionConfirmationResult
}

@ApplicationScoped
class AccountDeletionService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val fafTokenService: FafTokenService,
    private val fafProperties: FafProperties,
    private val accountAnonymizationService: AccountAnonymizationService,
    private val accountDeletionEventPublisher: AccountDeletionEventPublisher,
    private val transactionSynchronizationRegistry: TransactionSynchronizationRegistry,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(AccountDeletionService::class.java)
    }

    @Transactional
    fun requestAccountDeletion(userId: Int): AccountDeletionRequestResult {
        LOG.info("Account deletion requested for user id {}", userId)

        val user = userRepository.findById(userId)
            ?: return AccountDeletionRequestResult.UserNotFound.also {
                LOG.warn("Account deletion request rejected because user id {} was not found", userId)
            }

        createAccountDeletionRequest(user)
        LOG.info("Account deletion confirmation request created for user id {}", userId)

        return AccountDeletionRequestResult.ConfirmationSent
    }

    private fun createAccountDeletionRequest(user: User) {
        val userId = user.id ?: error("Cannot request account deletion for a user without an id")
        val lifetime = Duration.ofSeconds(fafProperties.account().accountDeletion().linkExpirationSeconds())

        val token = fafTokenService.createToken(
            FafToken.AccountDeletion(userId),
            lifetime,
        )

        val confirmationUrl = fafProperties.account().accountDeletion().confirmationUrlFormat().format(token)
        emailService.sendAccountDeletionConfirmationMail(user.username, user.email, confirmationUrl)
        LOG.info("Account deletion confirmation email queued for user id {}", userId)
    }

    @Transactional
    fun confirmAccountDeletion(token: String): AccountDeletionConfirmationResult {
        LOG.info("Account deletion confirmation received")

        val accountDeletionToken = try {
            fafTokenService.consumeToken(FafToken.AccountDeletion::class, token)
        } catch (exception: IllegalArgumentException) {
            LOG.info("Unable to consume account deletion token: {}", exception.message)
            return AccountDeletionConfirmationResult.InvalidToken
        }

        val userId = accountDeletionToken.userId

        val user = userRepository.findById(userId)
            ?: return AccountDeletionConfirmationResult.UserNotFound.also {
                transactionSynchronizationRegistry.setRollbackOnly()
                LOG.warn(
                    "Account deletion confirmation failed because user id {} was not found",
                    userId,
                )
            }

        try {
            LOG.info("Confirming account deletion for user id {}", user.id)

            val event = accountAnonymizationService.anonymizeUser(userId)
            LOG.info(
                "Local account anonymization completed for user id {}; publishing account deletion event",
                userId,
            )

            accountDeletionEventPublisher.publish(event)
            return AccountDeletionConfirmationResult.Confirmed
        } catch (exception: Exception) {
            transactionSynchronizationRegistry.setRollbackOnly()
            LOG.error("Failed to anonymize account for user id {}", userId, exception)
            return AccountDeletionConfirmationResult.AnonymizationFailed
        }
    }
}
