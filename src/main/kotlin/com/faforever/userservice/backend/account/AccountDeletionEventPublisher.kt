package com.faforever.userservice.backend.account

import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

@ApplicationScoped
class AccountDeletionEventPublisher(
    @param:Channel("account-deletion-events")
    private val emitter: Emitter<AccountDeletedEvent>,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AccountDeletionEventPublisher::class.java)
    }

    fun publish(event: AccountDeletedEvent) {
        LOG.info(
            "Publishing account deletion event for user id {}; occurredAt={}",
            event.userId,
            event.occurredAt,
        )


        val future = CompletableFuture<Void?>()

        val message = Message.of(
            event, Metadata.of(OutgoingRabbitMQMetadata.builder().withDeliveryMode(2).build()),
            {
                future.complete(null)
                CompletableFuture.completedFuture(null)
            }, { throwable ->
                future.completeExceptionally(throwable)
                CompletableFuture.completedFuture(null)
            })

        emitter.send(message)

        future.join()
    }
}
