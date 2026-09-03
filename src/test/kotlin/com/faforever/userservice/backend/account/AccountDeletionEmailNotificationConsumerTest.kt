package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.config.FafProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AccountDeletionEmailNotificationConsumerTest {
    private val emailService: EmailService = mock()
    private val fafProperties: FafProperties = mock()
    private val account: FafProperties.Account = mock()
    private val accountDeletion: FafProperties.Account.AccountDeletion = mock()

    private val consumer = AccountDeletionEmailNotificationConsumer(
        emailService,
        fafProperties,
    )

    @Test
    fun handleSendsAccountDeletionNotificationMail() {
        whenever(fafProperties.account()).thenReturn(account)
        whenever(account.accountDeletion()).thenReturn(accountDeletion)
        whenever(accountDeletion.externalConsumersEnabled()).thenReturn(true)

        val event = AccountDeletedEvent(
            userId = 1,
            username = "testUser",
            email = "test@example.com",
        )

        consumer.handle(event)

        verify(emailService).sendAccountDeletionNotificationMail(
            "testUser",
            "test@example.com",
        )
    }

    @Test
    fun handleSkipsAccountDeletionNotificationMailWhenExternalConsumersAreDisabled() {
        whenever(fafProperties.account()).thenReturn(account)
        whenever(account.accountDeletion()).thenReturn(accountDeletion)
        whenever(accountDeletion.externalConsumersEnabled()).thenReturn(false)

        val event = AccountDeletedEvent(
            userId = 1,
            username = "testUser",
            email = "test@example.com",
        )

        consumer.handle(event)

        verify(emailService, never()).sendAccountDeletionNotificationMail(
            "testUser",
            "test@example.com",
        )
    }
}
