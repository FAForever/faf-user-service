package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.i18n.I18n
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.ErrorHandler
import com.vaadin.flow.server.SessionInitEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ApplicationScoped
class NotificationErrorHandler(val i18n: I18n) : ErrorHandler {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(NotificationErrorHandler::class.java)
    }

    fun onSessionInit(@Observes event: SessionInitEvent) {
        event.session.errorHandler = this
    }

    override fun error(errorEvent: ErrorEvent) {
        logger.error("Unexpected exception", errorEvent.throwable)
        val ui = UI.getCurrent()
        ui?.access {
            val notification = Notification.show(i18n.getTranslation("error.internal", ui.locale))
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR)
        }
    }
}
