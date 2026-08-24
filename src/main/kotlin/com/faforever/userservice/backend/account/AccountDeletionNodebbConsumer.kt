package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.buffer.Buffer
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Invocation
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.UriBuilder
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.slf4j.LoggerFactory

@ApplicationScoped
class AccountDeletionNodebbConsumer(
    private val fafProperties: FafProperties,
    private val hmacService: HmacService,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AccountDeletionNodebbConsumer::class.java)
        private const val HMAC_HEADER = "X-HMAC"
        private val MAP_TYPE = object : TypeReference<Map<String, Any?>>() {}
    }

    @Incoming("account-deletion-nodebb")
    fun handle(payload: Buffer) {
        if (!fafProperties.account().accountDeletion().externalConsumersEnabled()) {
            LOG.info("Skipping NodeBB account deletion because external account deletion consumers are disabled.")
            return
        }

        val event = objectMapper.readValue(payload.bytes, AccountDeletedEvent::class.java)

        LOG.info("Deleting NodeBB account for deleted FAF user id {}", event.userId)

        val client = ClientBuilder.newClient()
        try {
            val readApiUrl = fafProperties.nodebb().readApiUrl().trimEnd('/')
            val writeApiUrl = fafProperties.nodebb().writeApiUrl().trimEnd('/')
            val token = fafProperties.nodebb().adminToken()

            val lookupResponse = client
                .target(
                    UriBuilder.fromUri("$readApiUrl/user/username/{username}")
                        .build(event.username),
                )
                .request()
                .withAccountDeletionHeaders(token)
                .get()

            when (lookupResponse.status) {
                200 -> {
                    val responseBody = lookupResponse.readEntity(String::class.java)
                    val body = objectMapper.readValue(responseBody, MAP_TYPE)
                    val uid = body["uid"] ?: error(
                        "NodeBB lookup response did not contain uid for FAF user id ${event.userId}",
                    )

                    LOG.info("Found NodeBB user id {} for deleted FAF user id {}", uid, event.userId)

                    val deleteResponse = client
                        .target("$writeApiUrl/users/$uid/account")
                        .request()
                        .withAccountDeletionHeaders(token)
                        .delete()

                    if (deleteResponse.status != 200) {
                        val errorBody = deleteResponse.readEntity(String::class.java)
                        error(
                            "NodeBB account deletion failed for FAF user id ${event.userId} " +
                                "with status ${deleteResponse.status}: $errorBody",
                        )
                    }

                    LOG.info("Deleted NodeBB account for FAF user id {}", event.userId)
                }

                404 -> LOG.info("NodeBB account not found for deleted FAF user id {}", event.userId)

                else -> {
                    val errorBody = lookupResponse.readEntity(String::class.java)
                    error(
                        "NodeBB lookup failed for FAF user id ${event.userId} " +
                            "with status ${lookupResponse.status}: $errorBody",
                    )
                }
            }
        } finally {
            client.close()
        }
    }

    private fun Invocation.Builder.withAccountDeletionHeaders(token: String): Invocation.Builder {
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")

        fafProperties.jwt().hmac()?.let { hmacConfig ->
            header(HMAC_HEADER, hmacService.generateHmacToken(hmacConfig.message(), hmacConfig.secret()))
        }

        return this
    }
}
