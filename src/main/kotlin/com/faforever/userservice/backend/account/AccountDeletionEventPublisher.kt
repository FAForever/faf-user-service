package com.faforever.userservice.backend.account

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.slf4j.LoggerFactory

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

        emitter.send(event).toCompletableFuture().join()
    }
}
