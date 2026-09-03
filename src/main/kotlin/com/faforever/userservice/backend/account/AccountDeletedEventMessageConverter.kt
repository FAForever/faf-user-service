package com.faforever.userservice.backend.account

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.MessageConverter
import io.vertx.core.buffer.Buffer
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Message
import java.lang.reflect.Type

@ApplicationScoped
class AccountDeletedEventMessageConverter(
    private val objectMapper: ObjectMapper,
) : MessageConverter {

    override fun canConvert(
        message: Message<*>,
        target: Type,
    ): Boolean =
        target == AccountDeletedEvent::class.java &&
            message.payload is Buffer

    override fun convert(
        message: Message<*>,
        target: Type,
    ): Message<*> {
        val payload = message.payload as Buffer

        val event = objectMapper.readValue(
            payload.bytes,
            AccountDeletedEvent::class.java,
        )

        return message.withPayload(event)
    }
}
