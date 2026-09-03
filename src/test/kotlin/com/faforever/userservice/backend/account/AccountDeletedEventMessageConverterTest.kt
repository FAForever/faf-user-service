package com.faforever.userservice.backend.account

import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.buffer.Buffer
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Message
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

@QuarkusTest
class AccountDeletedEventMessageConverterTest {
    @Inject
    private lateinit var converter: AccountDeletedEventMessageConverter

    @Test
    fun convertsBufferToAccountDeletedEvent() {
        val payload = Buffer.buffer(
            """
            {
              "userId": 1,
              "username": "testUser",
              "email": "test@example.com",
              "occurredAt": "2026-09-03T12:00:00+08:00"
            }
            """.trimIndent(),
        )

        val message = Message.of(payload)

        val converted = converter.convert(
            message,
            AccountDeletedEvent::class.java,
        )

        val event = converted.payload as AccountDeletedEvent

        assertThat(event.userId, equalTo(1))
        assertThat(event.username, equalTo("testUser"))
        assertThat(event.email, equalTo("test@example.com"))
        assertThat(
            event.occurredAt.toInstant(),
            equalTo(
                OffsetDateTime.parse(
                    "2026-09-03T12:00:00+08:00",
                ).toInstant(),
            ),
        )
    }

    @Test
    fun canConvertBufferToAccountDeletedEvent() {
        val message = Message.of(
            Buffer.buffer("{}"),
        )

        assertThat(
            converter.canConvert(
                message,
                AccountDeletedEvent::class.java,
            ),
            equalTo(true),
        )
    }

    @Test
    fun doesNotConvertBufferToDifferentTargetType() {
        val message = Message.of(
            Buffer.buffer("{}"),
        )

        assertThat(
            converter.canConvert(
                message,
                String::class.java,
            ),
            equalTo(false),
        )
    }

    @Test
    fun doesNotConvertNonBufferPayload() {
        val message = Message.of(
            """{"userId":1}""",
        )

        assertThat(
            converter.canConvert(
                message,
                AccountDeletedEvent::class.java,
            ),
            equalTo(false),
        )
    }
}
