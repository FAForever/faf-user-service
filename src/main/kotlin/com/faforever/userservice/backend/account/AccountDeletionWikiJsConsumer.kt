package com.faforever.userservice.backend.account

import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class AccountDeletionWikiJsConsumer(
    private val fafProperties: FafProperties,
    @RestClient private val wikiJsClient: WikiJsClient,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AccountDeletionWikiJsConsumer::class.java)
    }

    @Incoming("account-deletion-wikijs")
    fun handle(event: AccountDeletedEvent) {
        if (!fafProperties.account().accountDeletion().externalConsumersEnabled()) {
            LOG.info("Skipping WikiJS account deletion because external account deletion consumers are disabled.")
            return
        }

        LOG.info("Deleting WikiJS account for deleted FAF user id {}", event.userId)

        val wikiUserId = findWikiUserId(event.username, event.userId)

        if (wikiUserId == null) {
            LOG.info("WikiJS account not found for deleted FAF user id {}", event.userId)
            return
        }

        LOG.info("Found WikiJS user id {} for deleted FAF user id {}", wikiUserId, event.userId)
        deleteWikiUser(wikiUserId, event.userId)
        LOG.info("Deleted WikiJS account {} for FAF user id {}", wikiUserId, event.userId)
    }

    private fun findWikiUserId(username: String, userId: Int): Int? {
        val escapedUsername = escapeGraphqlString(username)
        val query = """
            {
              users {
                search(query: "$escapedUsername") {
                  id
                  name
                }
              }
            }
        """.trimIndent()

        val response = wikiJsClient.search(WikiJsGraphqlRequest(query))

        response.errors?.let { errors ->
            error("WikiJS GraphQL response contained errors for FAF user id $userId: $errors")
        }

        val users = response.data?.users?.search
            ?: error("WikiJS search response did not contain data.users.search for FAF user id $userId")

        val matches = users.filter { it.name == username }

        return when (matches.size) {
            0 -> null
            1 -> matches[0].id
            else -> error("WikiJS search returned ${matches.size} users named '$username' for FAF user id $userId")
        }
    }

    private fun escapeGraphqlString(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private fun deleteWikiUser(wikiUserId: Int, userId: Int) {
        val replaceUserId = fafProperties.wikijs().replaceUserId()
        val mutation = """
            mutation {
              users {
                delete(id: $wikiUserId, replaceId: $replaceUserId) {
                  responseResult {
                    succeeded
                    errorCode
                    message
                  }
                }
              }
            }
        """.trimIndent()

        val response = wikiJsClient.delete(WikiJsGraphqlRequest(mutation))

        response.errors?.let { errors ->
            error("WikiJS GraphQL response contained errors for FAF user id $userId: $errors")
        }

        val responseResult = response.data?.users?.delete?.responseResult
            ?: error("WikiJS delete response did not contain data.users.delete.responseResult for FAF user id $userId")

        if (!responseResult.succeeded) {
            error("WikiJS delete failed for FAF user id $userId: $responseResult")
        }
    }
}
