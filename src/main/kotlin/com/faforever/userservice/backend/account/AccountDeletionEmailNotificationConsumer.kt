package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.buffer.Buffer
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.slf4j.LoggerFactory

@ApplicationScoped
class AccountDeletionEmailNotificationConsumer(
    private val emailService: EmailService,
    private val fafProperties: FafProperties,
    private val objectMapper: ObjectMapper,
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(AccountDeletionEmailNotificationConsumer::class.java)
    }

    @Incoming("account-deletion-email-notification")
    fun handle(payload: Buffer) {
        if (!fafProperties.account().accountDeletion().externalConsumersEnabled()) {
            LOG.info(
                "Skipping account deletion notification email because " +
                    "external account deletion consumers are disabled.",
            )
            return
        }

        val event = objectMapper.readValue(payload.bytes, AccountDeletedEvent::class.java)

        LOG.info("Sending account deletion notification email for deleted FAF user id {}", event.userId)

        emailService.sendAccountDeletionNotificationMail(event.username, event.email)

        LOG.info("Sent account deletion notification email for deleted FAF user id {}", event.userId)
    }
}
