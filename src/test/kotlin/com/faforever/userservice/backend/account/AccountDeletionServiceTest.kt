package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.FafToken
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.config.FafProperties
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.temporal.TemporalAmount

@QuarkusTest
class AccountDeletionServiceTest {

    @Inject
    private lateinit var accountDeletionService: AccountDeletionService

    @Inject
    private lateinit var fafProperties: FafProperties

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var emailService: EmailService

    @InjectMock
    private lateinit var fafTokenService: FafTokenService

    @InjectMock
    private lateinit var accountAnonymizationService: AccountAnonymizationService

    @InjectMock
    private lateinit var accountDeletionEventPublisher: AccountDeletionEventPublisher

    @Test
    fun requestAccountDeletionCreatesTokenAndSendsConfirmationEmail() {
        val user = buildTestUser()
        val token = "token"
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(
            fafTokenService.createToken(
                eq(FafToken.AccountDeletion(user.id!!)),
                any<TemporalAmount>(),
            ),
        ).thenReturn(token)

        val result = accountDeletionService.requestAccountDeletion(user.id!!)

        assertThat(result, equalTo(AccountDeletionRequestResult.ConfirmationSent))
        verify(fafTokenService).createToken(
            eq(FafToken.AccountDeletion(user.id!!)),
            any<TemporalAmount>(),
        )
        verify(emailService).sendAccountDeletionConfirmationMail(
            user.username,
            user.email,
            fafProperties.account().accountDeletion().confirmationUrlFormat().format(token),
        )
    }

    @Test
    fun requestAccountDeletionReturnsUserNotFound() {
        val userId = 1
        whenever(userRepository.findById(userId)).thenReturn(null)

        val result = accountDeletionService.requestAccountDeletion(userId)

        assertThat(result, equalTo(AccountDeletionRequestResult.UserNotFound))
        verifyNoInteractions(emailService)
        verifyNoInteractions(fafTokenService)
    }

    @Test
    fun confirmAccountDeletionConsumesTokenAnonymizesAccountAndPublishesEvent() {
        val user = buildTestUser()
        val token = "token"
        val event = AccountDeletedEvent(
            userId = user.id!!,
            username = user.username,
            email = user.email,
        )
        whenever(fafTokenService.consumeToken(FafToken.AccountDeletion::class, token))
            .thenReturn(FafToken.AccountDeletion(user.id!!))
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(accountAnonymizationService.anonymizeUser(user.id!!)).thenReturn(event)

        val result = accountDeletionService.confirmAccountDeletion(token)

        assertThat(result, equalTo(AccountDeletionConfirmationResult.Confirmed))
        verify(fafTokenService).consumeToken(FafToken.AccountDeletion::class, token)
        verify(accountAnonymizationService).anonymizeUser(user.id!!)
        verify(accountDeletionEventPublisher).publish(event)
    }

    @Test
    fun confirmAccountDeletionRejectsInvalidTokenWhenTokenConsumptionFails() {
        val token = "token"
        whenever(fafTokenService.consumeToken(FafToken.AccountDeletion::class, token))
            .thenThrow(IllegalArgumentException("Token not found"))

        val result = accountDeletionService.confirmAccountDeletion(token)

        assertThat(result, equalTo(AccountDeletionConfirmationResult.InvalidToken))
        verify(fafTokenService).consumeToken(FafToken.AccountDeletion::class, token)
        verifyNoInteractions(userRepository)
        verifyNoInteractions(accountAnonymizationService)
        verifyNoInteractions(accountDeletionEventPublisher)
    }

    @Test
    fun confirmAccountDeletionReturnsUserNotFoundIfUserNoLongerExists() {
        val user = buildTestUser()
        val token = "token"

        whenever(fafTokenService.consumeToken(FafToken.AccountDeletion::class, token))
            .thenReturn(FafToken.AccountDeletion(user.id!!))
        whenever(userRepository.findById(user.id!!)).thenReturn(null)

        val result = accountDeletionService.confirmAccountDeletion(token)

        assertThat(result, equalTo(AccountDeletionConfirmationResult.UserNotFound))
        verify(fafTokenService).consumeToken(FafToken.AccountDeletion::class, token)
        verifyNoInteractions(accountAnonymizationService)
        verifyNoInteractions(accountDeletionEventPublisher)
    }

    @Test
    fun confirmAccountDeletionPropagatesAnonymizationFailure() {
        val user = buildTestUser()
        val token = "token"

        whenever(fafTokenService.consumeToken(FafToken.AccountDeletion::class, token))
            .thenReturn(FafToken.AccountDeletion(user.id!!))
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(accountAnonymizationService.anonymizeUser(user.id!!))
            .thenThrow(RuntimeException("anonymization failed"))

        val exception = assertThrows<RuntimeException> {
            accountDeletionService.confirmAccountDeletion(token)
        }

        assertThat(exception.message, equalTo("anonymization failed"))
        verify(fafTokenService).consumeToken(FafToken.AccountDeletion::class, token)
        verify(accountAnonymizationService).anonymizeUser(user.id!!)
        verifyNoInteractions(accountDeletionEventPublisher)
    }

    @Test
    fun confirmAccountDeletionPropagatesPublishingFailure() {
        val user = buildTestUser()
        val token = "token"
        val event = AccountDeletedEvent(
            userId = user.id!!,
            username = user.username,
            email = user.email,
        )

        whenever(fafTokenService.consumeToken(FafToken.AccountDeletion::class, token))
            .thenReturn(FafToken.AccountDeletion(user.id!!))
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(accountAnonymizationService.anonymizeUser(user.id!!)).thenReturn(event)

        doThrow(RuntimeException("publishing failed"))
            .whenever(accountDeletionEventPublisher)
            .publish(event)

        val exception = assertThrows<RuntimeException> {
            accountDeletionService.confirmAccountDeletion(token)
        }

        assertThat(exception.message, equalTo("publishing failed"))
        verify(fafTokenService).consumeToken(FafToken.AccountDeletion::class, token)
        verify(accountAnonymizationService).anonymizeUser(user.id!!)
        verify(accountDeletionEventPublisher).publish(event)
    }

    private fun buildTestUser(
        id: Int = 1,
        username: String = "testUser",
        email: String = "old@example.com",
    ) = User(
        id = id,
        username = username,
        password = "password",
        email = email,
        ip = null,
        acceptedTos = null,
    )
}
