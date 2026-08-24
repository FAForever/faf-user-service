package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpServer
import io.vertx.core.buffer.Buffer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class AccountDeletionWikiJsConsumerTest {
    private var server: HttpServer? = null
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun handleDeletesWikiJsUserWhenUserExists() {
        val requestCount = AtomicInteger(0)
        val authorizationHeader = AtomicReference<String>()
        val hmacHeader = AtomicReference<String>()
        val deleteMutationBody = AtomicReference<String>()

        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/graphql") { exchange ->
                authorizationHeader.set(exchange.requestHeaders.getFirst("Authorization"))
                hmacHeader.set(exchange.requestHeaders.getFirst("X-HMAC"))

                val requestBody = exchange.requestBody.bufferedReader().readText()
                val count = requestCount.incrementAndGet()

                val response = if (count == 1) {
                    """
                    {
                      "data": {
                        "users": {
                          "search": [
                            {
                              "id": 123,
                              "name": "testUser"
                            }
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                } else {
                    deleteMutationBody.set(requestBody)
                    """
                    {
                      "data": {
                        "users": {
                          "delete": {
                            "responseResult": {
                              "succeeded": true,
                              "errorCode": 0,
                              "message": "ok"
                            }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                }

                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionWikiJsConsumer(
            fafProperties = buildProperties("http://localhost:$port/graphql"),
            hmacService = HmacService(),
            objectMapper = objectMapper,
        )

        consumer.handle(
            toPayload(
                AccountDeletedEvent(
                    userId = 1,
                    username = "testUser",
                    email = "test@example.com",
                ),
            ),
        )

        assertThat(requestCount.get(), equalTo(2))
        assertThat(authorizationHeader.get(), equalTo("Bearer wikijs-token"))
        assertThat(hmacHeader.get(), containsString("-"))
        assertThat(deleteMutationBody.get(), containsString("delete(id: 123, replaceId: 42)"))
    }

    @Test
    fun handleDoesNothingWhenWikiJsUserDoesNotExist() {
        val requestCount = AtomicInteger(0)

        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/graphql") { exchange ->
                requestCount.incrementAndGet()

                val response = """
                    {
                      "data": {
                        "users": {
                          "search": []
                        }
                      }
                    }
                """.trimIndent()

                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionWikiJsConsumer(
            fafProperties = buildProperties("http://localhost:$port/graphql"),
            hmacService = HmacService(),
            objectMapper = objectMapper,
        )

        consumer.handle(
            toPayload(
                AccountDeletedEvent(
                    userId = 1,
                    username = "testUser",
                    email = "test@example.com",
                ),
            ),
        )

        assertThat(requestCount.get(), equalTo(1))
    }

    @Test
    fun handleThrowsWhenWikiJsSearchReturnsMultipleUsers() {
        val requestCount = AtomicInteger(0)

        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/graphql") { exchange ->
                requestCount.incrementAndGet()

                val response = """
                    {
                      "data": {
                        "users": {
                          "search": [
                            {
                              "id": 123,
                              "name": "testUser"
                            },
                            {
                              "id": 456,
                              "name": "testUser"
                            }
                          ]
                        }
                      }
                    }
                """.trimIndent()

                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionWikiJsConsumer(
            fafProperties = buildProperties("http://localhost:$port/graphql"),
            hmacService = HmacService(),
            objectMapper = objectMapper,
        )

        try {
            consumer.handle(
                toPayload(
                    AccountDeletedEvent(
                        userId = 1,
                        username = "testUser",
                        email = "test@example.com",
                    ),
                ),
            )
        } catch (exception: IllegalStateException) {
            assertThat(exception.message, containsString("2 users named 'testUser'"))
            assertThat(requestCount.get(), equalTo(1))
            return
        }

        error("Expected WikiJS ambiguous user failure")
    }

    @Test
    fun handleThrowsWhenWikiJsDeleteFails() {
        val requestCount = AtomicInteger(0)

        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/graphql") { exchange ->
                val count = requestCount.incrementAndGet()

                val response = if (count == 1) {
                    """
                    {
                      "data": {
                        "users": {
                          "search": [
                            {
                              "id": 123,
                              "name": "testUser"
                            }
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                } else {
                    """
                    {
                      "data": {
                        "users": {
                          "delete": {
                            "responseResult": {
                              "succeeded": false,
                              "errorCode": 500,
                              "message": "delete failed"
                            }
                          }
                        }
                      }
                    }
                    """.trimIndent()
                }

                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionWikiJsConsumer(
            fafProperties = buildProperties("http://localhost:$port/graphql"),
            hmacService = HmacService(),
            objectMapper = objectMapper,
        )

        try {
            consumer.handle(
                toPayload(
                    AccountDeletedEvent(
                        userId = 1,
                        username = "testUser",
                        email = "test@example.com",
                    ),
                ),
            )
        } catch (exception: IllegalStateException) {
            assertThat(exception.message, containsString("WikiJS delete failed"))
            return
        }

        error("Expected WikiJS delete failure")
    }

    @Test
    fun handleThrowsWhenWikiJsReturnsGraphqlErrors() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/graphql") { exchange ->
                val response = """
                    {
                      "errors": [
                        {
                          "message": "graphql failed"
                        }
                      ]
                    }
                """.trimIndent()

                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionWikiJsConsumer(
            fafProperties = buildProperties("http://localhost:$port/graphql"),
            hmacService = HmacService(),
            objectMapper = objectMapper,
        )

        try {
            consumer.handle(
                toPayload(
                    AccountDeletedEvent(
                        userId = 1,
                        username = "testUser",
                        email = "test@example.com",
                    ),
                ),
            )
        } catch (exception: IllegalStateException) {
            assertThat(exception.message, containsString("WikiJS GraphQL response contained errors"))
            return
        }

        error("Expected WikiJS GraphQL error")
    }

    private fun buildProperties(graphqlUrl: String): FafProperties {
        val properties = mock<FafProperties>()
        val account = mock<FafProperties.Account>()
        val accountDeletion = mock<FafProperties.Account.AccountDeletion>()
        val wikijs = mock<FafProperties.WikiJs>()
        val jwt = mock<FafProperties.Jwt>()
        val hmac = mock<FafProperties.Hmac>()

        whenever(properties.account()).thenReturn(account)
        whenever(account.accountDeletion()).thenReturn(accountDeletion)
        whenever(accountDeletion.externalConsumersEnabled()).thenReturn(true)

        whenever(properties.wikijs()).thenReturn(wikijs)
        whenever(wikijs.graphqlUrl()).thenReturn(graphqlUrl)
        whenever(wikijs.token()).thenReturn("wikijs-token")
        whenever(wikijs.replaceUserId()).thenReturn(42)

        whenever(properties.jwt()).thenReturn(jwt)
        whenever(jwt.hmac()).thenReturn(hmac)
        whenever(hmac.message()).thenReturn("helloFaf")
        whenever(hmac.secret()).thenReturn("banana")

        return properties
    }

    private fun toPayload(event: AccountDeletedEvent): Buffer =
        Buffer.buffer(objectMapper.writeValueAsBytes(event))
}
