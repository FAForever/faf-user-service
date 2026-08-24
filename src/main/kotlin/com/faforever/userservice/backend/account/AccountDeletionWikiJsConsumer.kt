package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.buffer.Buffer
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.slf4j.LoggerFactory

@ApplicationScoped
class AccountDeletionWikiJsConsumer(
    private val fafProperties: FafProperties,
    private val hmacService: HmacService,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AccountDeletionWikiJsConsumer::class.java)
        private const val HMAC_HEADER = "X-HMAC"
        private val MAP_TYPE = object : TypeReference<Map<String, Any?>>() {}
    }

    @Incoming("account-deletion-wikijs")
    fun handle(payload: Buffer) {
        if (!fafProperties.account().accountDeletion().externalConsumersEnabled()) {
            LOG.info("Skipping WikiJS account deletion because external account deletion consumers are disabled.")
            return
        }

        val event = objectMapper.readValue(payload.bytes, AccountDeletedEvent::class.java)

        LOG.info("Deleting WikiJS account for deleted FAF user id {}", event.userId)

        val client = ClientBuilder.newClient()
        try {
            val wikiUserId = findWikiUserId(client, event.username, event.userId)

            if (wikiUserId == null) {
                LOG.info("WikiJS account not found for deleted FAF user id {}", event.userId)
                return
            }

            LOG.info("Found WikiJS user id {} for deleted FAF user id {}", wikiUserId, event.userId)

            deleteWikiUser(client, wikiUserId, event.userId)

            LOG.info("Deleted WikiJS account {} for FAF user id {}", wikiUserId, event.userId)
        } finally {
            client.close()
        }
    }

    private fun findWikiUserId(client: jakarta.ws.rs.client.Client, username: String, userId: Int): Int? {
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

        val response = executeGraphql(client, query, userId)
        val users = (((response["data"] as? Map<*, *>)?.get("users") as? Map<*, *>)?.get("search") as? List<*>)
            ?: error("WikiJS search response did not contain data.users.search for FAF user id $userId")

        val matches = users
            .mapNotNull { it as? Map<*, *> }
            .filter { it["name"] == username }

        return when (matches.size) {
            0 -> null
            1 -> matches[0]["id"]?.toString()?.toInt()
            else -> error("WikiJS search returned ${matches.size} users named '$username' for FAF user id $userId")
        }
    }

    private fun escapeGraphqlString(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private fun deleteWikiUser(client: jakarta.ws.rs.client.Client, wikiUserId: Int, userId: Int) {
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

        val response = executeGraphql(client, mutation, userId)
        val data = response["data"] as? Map<*, *>
        val users = data?.get("users") as? Map<*, *>
        val delete = users?.get("delete") as? Map<*, *>
        val responseResult = delete?.get("responseResult") as? Map<*, *> ?: error(
            "WikiJS delete response did not contain data.users.delete.responseResult for FAF user id $userId",
        )

        if (responseResult["succeeded"] != true) {
            error("WikiJS delete failed for FAF user id $userId: $responseResult")
        }
    }

    private fun executeGraphql(client: jakarta.ws.rs.client.Client, query: String, userId: Int): Map<String, Any?> {
        val body = mapOf("query" to query)
        val request = client
            .target(fafProperties.wikijs().graphqlUrl())
            .request(MediaType.APPLICATION_JSON_TYPE)
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${fafProperties.wikijs().token()}")

        fafProperties.jwt().hmac()?.let { hmacConfig ->
            request.header(HMAC_HEADER, hmacService.generateHmacToken(hmacConfig.message(), hmacConfig.secret()))
        }

        val response = request.post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE))
        val responseBody = response.readEntity(String::class.java)

        if (response.status !in 200..299) {
            error(
                "WikiJS GraphQL request failed for FAF user id $userId " +
                    "with status ${response.status}: $responseBody",
            )
        }

        val parsedResponse = objectMapper.readValue(responseBody, MAP_TYPE)

        if (parsedResponse["errors"] != null) {
            error(
                "WikiJS GraphQL response contained errors for FAF user id $userId: " +
                    "${parsedResponse["errors"]}",
            )
        }

        return parsedResponse
    }
}
