package com.faforever.userservice.backend.ucp

import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Status
import jakarta.transaction.Synchronization
import jakarta.transaction.TransactionSynchronizationRegistry
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

@ApplicationScoped
class PlayerAvatarUpdatePublisher(
    @param:Channel("player-avatar-update")
    private val emitter: Emitter<PlayerAvatarUpdateEvent>,
    private val transactionSynchronizationRegistry: TransactionSynchronizationRegistry,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(PlayerAvatarUpdatePublisher::class.java)
    }

    fun publish(playerId: Int, avatarId: Int?) {
        if (transactionSynchronizationRegistry.transactionStatus == Status.STATUS_ACTIVE) {
            transactionSynchronizationRegistry.registerInterposedSynchronization(
                object : Synchronization {
                    override fun beforeCompletion() {}
                    override fun afterCompletion(status: Int) {
                        if (status == Status.STATUS_COMMITTED) {
                            send(playerId, avatarId)
                        }
                    }
                },
            )
        } else {
            send(playerId, avatarId)
        }
    }

    private fun send(playerId: Int, avatarId: Int?) {
        LOG.debug("Publishing player_avatar update: player_id={}, avatar_id={}", playerId, avatarId)

        val future = CompletableFuture<Void?>()
        val message = Message.of(
            PlayerAvatarUpdateEvent(playerId, avatarId),
            Metadata.of(OutgoingRabbitMQMetadata.builder().withDeliveryMode(2).build()),
            {
                future.complete(null)
                CompletableFuture.completedFuture(null)
            },
            { throwable ->
                future.completeExceptionally(throwable)
                CompletableFuture.completedFuture(null)
            },
        )

        emitter.send(message)
        future.join()
    }
}
