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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class AccountDeletionNodebbConsumerTest {
    private var server: HttpServer? = null
    private val objectMapper = ObjectMapper().findAndRegisterModules()

    @AfterEach
    fun tearDown() {
        server?.stop(0)
    }

    @Test
    fun handleDeletesNodebbUserWhenUserExists() {
        val lookupAuthorizationHeader = AtomicReference<String>()
        val lookupHmacHeader = AtomicReference<String>()
        val deleteAuthorizationHeader = AtomicReference<String>()
        val deleteHmacHeader = AtomicReference<String>()
        val deleteCalled = AtomicBoolean(false)

        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/api/user/username/testUser") { exchange ->
                lookupAuthorizationHeader.set(exchange.requestHeaders.getFirst("Authorization"))
                lookupHmacHeader.set(exchange.requestHeaders.getFirst("X-HMAC"))

                val response = """{"uid":123}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            createContext("/api/v3/users/123/account") { exchange ->
                deleteCalled.set(true)
                deleteAuthorizationHeader.set(exchange.requestHeaders.getFirst("Authorization"))
                deleteHmacHeader.set(exchange.requestHeaders.getFirst("X-HMAC"))

                exchange.sendResponseHeaders(200, -1)
                exchange.close()
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionNodebbConsumer(
            fafProperties = buildProperties(
                readApiUrl = "http://localhost:$port/api",
                writeApiUrl = "http://localhost:$port/api/v3",
            ),
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

        assertThat(deleteCalled.get(), equalTo(true))
        assertThat(lookupAuthorizationHeader.get(), equalTo("Bearer nodebb-token"))
        assertThat(deleteAuthorizationHeader.get(), equalTo("Bearer nodebb-token"))
        assertThat(lookupHmacHeader.get(), containsString("-"))
        assertThat(deleteHmacHeader.get(), containsString("-"))
    }

    @Test
    fun handleDoesNothingWhenNodebbUserDoesNotExist() {
        val deleteCalled = AtomicBoolean(false)

        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/api/user/username/testUser") { exchange ->
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
            }

            createContext("/api/v3/users/123/account") { exchange ->
                deleteCalled.set(true)
                exchange.sendResponseHeaders(200, -1)
                exchange.close()
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionNodebbConsumer(
            fafProperties = buildProperties(
                readApiUrl = "http://localhost:$port/api",
                writeApiUrl = "http://localhost:$port/api/v3",
            ),
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

        assertThat(deleteCalled.get(), equalTo(false))
    }

    @Test
    fun handleThrowsWhenNodebbDeleteFails() {
        server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/api/user/username/testUser") { exchange ->
                val response = """{"uid":123}"""
                exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            createContext("/api/v3/users/123/account") { exchange ->
                val response = "delete failed"
                exchange.sendResponseHeaders(500, response.toByteArray().size.toLong())
                exchange.responseBody.use { it.write(response.toByteArray()) }
            }

            start()
        }

        val port = server!!.address.port
        val consumer = AccountDeletionNodebbConsumer(
            fafProperties = buildProperties(
                readApiUrl = "http://localhost:$port/api",
                writeApiUrl = "http://localhost:$port/api/v3",
            ),
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
            assertThat(exception.message, containsString("NodeBB account deletion failed"))
            return
        }

        error("Expected NodeBB delete failure")
    }

    private fun buildProperties(
        readApiUrl: String,
        writeApiUrl: String,
    ): FafProperties {
        val properties = mock<FafProperties>()
        val account = mock<FafProperties.Account>()
        val accountDeletion = mock<FafProperties.Account.AccountDeletion>()
        val nodebb = mock<FafProperties.Nodebb>()
        val jwt = mock<FafProperties.Jwt>()
        val hmac = mock<FafProperties.Hmac>()

        whenever(properties.account()).thenReturn(account)
        whenever(account.accountDeletion()).thenReturn(accountDeletion)
        whenever(accountDeletion.externalConsumersEnabled()).thenReturn(true)

        whenever(properties.nodebb()).thenReturn(nodebb)
        whenever(nodebb.readApiUrl()).thenReturn(readApiUrl)
        whenever(nodebb.writeApiUrl()).thenReturn(writeApiUrl)
        whenever(nodebb.adminToken()).thenReturn("nodebb-token")

        whenever(properties.jwt()).thenReturn(jwt)
        whenever(jwt.hmac()).thenReturn(hmac)
        whenever(hmac.message()).thenReturn("helloFaf")
        whenever(hmac.secret()).thenReturn("banana")

        return properties
    }

    private fun toPayload(event: AccountDeletedEvent): Buffer =
        Buffer.buffer(objectMapper.writeValueAsBytes(event))
}
