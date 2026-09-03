package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.account.AccountDeletionRequestResult
import com.faforever.userservice.backend.account.AccountDeletionService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/delete-account", layout = UcpLayout::class)
@PermitAll
class UcpDeleteAccountView(
    private val ucpSessionService: UcpSessionService,
    private val accountDeletionService: AccountDeletionService,
) : VerticalLayout() {

    private val acknowledgement = Checkbox(
        getTranslation("ucp.deleteAccount.acknowledgement"),
    ).apply {
        setWidthFull()
    }

    private val submit = Button(getTranslation("ucp.deleteAccount.submit")) { requestAccountDeletion() }.apply {
        addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY)
    }

    init {
        setPadding(true)
        setSizeFull()

        add(
            H2(getTranslation("ucp.nav.deleteAccount")),
            Paragraph(getTranslation("ucp.deleteAccount.description")),
            Paragraph(getTranslation("ucp.deleteAccount.warning")),
            acknowledgement,
            submit,
        )
    }

    private fun requestAccountDeletion() {
        if (!acknowledgement.value) {
            showError(getTranslation("ucp.deleteAccount.mustAcknowledge"))
            return
        }

        val currentUser = ucpSessionService.getCurrentUser()
        when (accountDeletionService.requestAccountDeletion(currentUser.userId)) {
            AccountDeletionRequestResult.ConfirmationSent -> {
                Dialog().apply {
                    add(H2(getTranslation("ucp.deleteAccount.sent.title")))
                    add(Span(getTranslation("ucp.deleteAccount.sent.details")))
                    footer.add(
                        Button(getTranslation("ucp.deleteAccount.sent.close")) { close() }.apply {
                            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                        },
                    )
                    open()
                }
                acknowledgement.value = false
            }

            AccountDeletionRequestResult.UserNotFound -> showError(getTranslation("ucp.deleteAccount.userNotFound"))
        }
    }

    private fun showError(message: String) {
        Notification.show(message, 5000, Notification.Position.MIDDLE).apply {
            addThemeVariants(NotificationVariant.LUMO_ERROR)
        }
    }
}
