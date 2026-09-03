package com.faforever.userservice.backend.account

import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.LoggerFactory

@ApplicationScoped
class AccountDeletionNodebbConsumer(
    private val fafProperties: FafProperties,
    @RestClient private val nodebbReadClient: NodebbReadClient,
    @RestClient private val nodebbWriteClient: NodebbWriteClient,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AccountDeletionNodebbConsumer::class.java)
    }

    @Incoming("account-deletion-nodebb")
    fun handle(event: AccountDeletedEvent) {
        if (!fafProperties.account().accountDeletion().externalConsumersEnabled()) {
            LOG.info("Skipping NodeBB account deletion because external account deletion consumers are disabled.")
            return
        }
        LOG.info("Deleting NodeBB account for deleted FAF user id {}", event.userId)

        val uid = try {
            nodebbReadClient.getUserByUsername(event.username).uid
        } catch (exception: NodebbUserNotFoundException) {
            LOG.info("NodeBB account not found for deleted FAF user id {}", event.userId)
            return
        }

        LOG.info("Found NodeBB user id {} for deleted FAF user id {}", uid, event.userId)
        nodebbWriteClient.deleteAccount(uid)
        LOG.info("Deleted NodeBB account for FAF user id {}", event.userId)
    }
}
