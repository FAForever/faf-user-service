package com.faforever.userservice.backend.account

import com.faforever.userservice.config.FafProperties
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AccountDeletionWikiJsConsumerTest {
    private val fafProperties: FafProperties = mock()
    private val wikiJsClient: WikiJsClient = mock()

    private val consumer = AccountDeletionWikiJsConsumer(
        fafProperties = fafProperties,
        wikiJsClient = wikiJsClient,
    )

    @Test
    fun handleDeletesWikiJsUserWhenUserExists() {
        mockExternalConsumersEnabled(true)

        whenever(wikiJsClient.search(any()))
            .thenReturn(searchResponse(WikiJsUser(123, "testUser")))

        whenever(wikiJsClient.delete(any()))
            .thenReturn(deleteResponse(succeeded = true))

        consumer.handle(event())

        verify(wikiJsClient).search(
            argThat {
                query.contains("search(query: \"testUser\")")
            },
        )

        verify(wikiJsClient).delete(
            argThat {
                query.contains("delete(id: 123, replaceId: 42)")
            },
        )
    }

    @Test
    fun handleDoesNothingWhenWikiJsUserDoesNotExist() {
        mockExternalConsumersEnabled(true)

        whenever(wikiJsClient.search(any())).thenReturn(
            WikiJsSearchResponse(
                data = WikiJsSearchResponse.SearchData(
                    WikiJsSearchResponse.SearchUsers(emptyList()),
                ),
                errors = null,
            ),
        )

        consumer.handle(event())

        verify(wikiJsClient, never()).delete(any())
    }

    @Test
    fun handleThrowsWhenWikiJsSearchReturnsMultipleUsers() {
        mockExternalConsumersEnabled(true)

        whenever(wikiJsClient.search(any())).thenReturn(
            searchResponse(
                WikiJsUser(123, "testUser"),
                WikiJsUser(456, "testUser"),
            ),
        )

        val exception = assertThrows<IllegalStateException> {
            consumer.handle(event())
        }

        assertThat(
            exception.message,
            containsString("2 users named 'testUser'"),
        )

        verify(wikiJsClient, never()).delete(any())
    }

    @Test
    fun handleThrowsWhenWikiJsDeleteFails() {
        mockExternalConsumersEnabled(true)

        whenever(wikiJsClient.search(any()))
            .thenReturn(searchResponse(WikiJsUser(123, "testUser")))

        whenever(wikiJsClient.delete(any()))
            .thenReturn(deleteResponse(succeeded = false))

        val exception = assertThrows<IllegalStateException> {
            consumer.handle(event())
        }

        assertThat(
            exception.message,
            containsString("WikiJS delete failed"),
        )
    }

    @Test
    fun handleThrowsWhenWikiJsReturnsGraphqlErrors() {
        mockExternalConsumersEnabled(true)

        whenever(wikiJsClient.search(any())).thenReturn(
            WikiJsSearchResponse(
                data = null,
                errors = listOf(
                    WikiJsGraphqlError("graphql failed"),
                ),
            ),
        )

        val exception = assertThrows<IllegalStateException> {
            consumer.handle(event())
        }

        assertThat(
            exception.message,
            containsString("WikiJS GraphQL response contained errors"),
        )
    }

    @Test
    fun handleThrowsWhenWikiJsDeleteReturnsGraphqlErrors() {
        mockExternalConsumersEnabled(true)

        whenever(wikiJsClient.search(any()))
            .thenReturn(searchResponse(WikiJsUser(123, "testUser")))

        whenever(wikiJsClient.delete(any())).thenReturn(
            WikiJsDeleteResponse(
                data = null,
                errors = listOf(
                    WikiJsGraphqlError("graphql failed"),
                ),
            ),
        )

        val exception = assertThrows<IllegalStateException> {
            consumer.handle(event())
        }

        assertThat(
            exception.message,
            containsString("WikiJS GraphQL response contained errors"),
        )
    }

    @Test
    fun handleSkipsWhenExternalConsumersAreDisabled() {
        mockExternalConsumersEnabled(false)

        consumer.handle(event())

        verify(wikiJsClient, never()).search(any())
        verify(wikiJsClient, never()).delete(any())
    }

    private fun mockExternalConsumersEnabled(enabled: Boolean) {
        val account = mock<FafProperties.Account>()
        val accountDeletion = mock<FafProperties.Account.AccountDeletion>()
        val wikiJs = mock<FafProperties.WikiJs>()

        whenever(fafProperties.account()).thenReturn(account)
        whenever(account.accountDeletion()).thenReturn(accountDeletion)
        whenever(accountDeletion.externalConsumersEnabled()).thenReturn(enabled)
        whenever(fafProperties.wikijs()).thenReturn(wikiJs)
        whenever(wikiJs.replaceUserId()).thenReturn(42)
    }

    private fun searchResponse(
        vararg users: WikiJsUser,
    ): WikiJsSearchResponse =
        WikiJsSearchResponse(
            data = WikiJsSearchResponse.SearchData(
                WikiJsSearchResponse.SearchUsers(
                    users.toList(),
                ),
            ),
            errors = null,
        )

    private fun deleteResponse(
        succeeded: Boolean,
    ): WikiJsDeleteResponse =
        WikiJsDeleteResponse(
            data = WikiJsDeleteResponse.DeleteData(
                WikiJsDeleteResponse.DeleteUsers(
                    WikiJsDeleteResponse.DeleteResult(
                        WikiJsDeleteResponse.ResponseResult(
                            succeeded = succeeded,
                            errorCode = if (succeeded) 0 else 500,
                            message = if (succeeded) "ok" else "delete failed",
                        ),
                    ),
                ),
            ),
            errors = null,
        )

    private fun event(): AccountDeletedEvent =
        AccountDeletedEvent(
            userId = 1,
            username = "testUser",
            email = "test@example.com",
        )
}
