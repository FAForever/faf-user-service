package com.faforever.userservice.backend.account

import com.faforever.userservice.config.FafProperties
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AccountDeletionNodebbConsumerTest {
    private val fafProperties: FafProperties = mock()
    private val nodebbReadClient: NodebbReadClient = mock()
    private val nodebbWriteClient: NodebbWriteClient = mock()

    private val consumer = AccountDeletionNodebbConsumer(
        fafProperties = fafProperties,
        nodebbReadClient = nodebbReadClient,
        nodebbWriteClient = nodebbWriteClient,
    )

    @Test
    fun handleDeletesNodebbUserWhenUserExists() {
        mockExternalConsumersEnabled(true)

        whenever(nodebbReadClient.getUserByUsername("testUser"))
            .thenReturn(NodebbUser(123))

        consumer.handle(event())

        verify(nodebbReadClient).getUserByUsername("testUser")
        verify(nodebbWriteClient).deleteAccount(123)
    }

    @Test
    fun handleDoesNothingWhenNodebbUserDoesNotExist() {
        mockExternalConsumersEnabled(true)

        whenever(nodebbReadClient.getUserByUsername("testUser"))
            .thenThrow(NodebbUserNotFoundException())

        consumer.handle(event())

        verify(nodebbReadClient).getUserByUsername("testUser")
        verify(nodebbWriteClient, never()).deleteAccount(any())
    }

    @Test
    fun handleThrowsWhenNodebbDeleteFails() {
        mockExternalConsumersEnabled(true)

        whenever(nodebbReadClient.getUserByUsername("testUser"))
            .thenReturn(NodebbUser(123))

        doThrow(
            NodebbAccountDeletionFailedException(
                "NodeBB account deletion failed with status 500: delete failed",
            ),
        )
            .whenever(nodebbWriteClient)
            .deleteAccount(123)

        assertThrows<NodebbAccountDeletionFailedException> {
            consumer.handle(event())
        }
    }

    @Test
    fun handleSkipsWhenExternalConsumersAreDisabled() {
        mockExternalConsumersEnabled(false)

        consumer.handle(event())

        verify(nodebbReadClient, never()).getUserByUsername(any())
        verify(nodebbWriteClient, never()).deleteAccount(any())
    }

    private fun mockExternalConsumersEnabled(enabled: Boolean) {
        val account = mock<FafProperties.Account>()
        val accountDeletion = mock<FafProperties.Account.AccountDeletion>()

        whenever(fafProperties.account()).thenReturn(account)
        whenever(account.accountDeletion()).thenReturn(accountDeletion)
        whenever(accountDeletion.externalConsumersEnabled()).thenReturn(enabled)
    }

    private fun event(): AccountDeletedEvent =
        AccountDeletedEvent(
            userId = 1,
            username = "testUser",
            email = "test@example.com",
        )
}
