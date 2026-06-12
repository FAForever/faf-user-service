package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.account.EmailChangeRequestResult
import com.faforever.userservice.backend.account.EmailChangeService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.EmailField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/email", layout = UcpLayout::class)
@PermitAll
class UcpChangeEmailView(
    private val ucpSessionService: UcpSessionService,
    private val emailChangeService: EmailChangeService,
) : VerticalLayout() {

    private val newEmail = EmailField(getTranslation("ucp.changeEmail.newEmail")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
        isRequiredIndicatorVisible = true
    }

    private val submit = Button(getTranslation("ucp.changeEmail.submit")) { requestEmailChange() }.apply {
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
    }

    init {
        setPadding(true)
        setSizeFull()
        add(
            H2(getTranslation("ucp.nav.changeEmail")),
            Paragraph(getTranslation("ucp.changeEmail.description")),
            newEmail,
            submit,
        )
    }

    private fun requestEmailChange() {
        if (newEmail.value.isNullOrBlank()) {
            showError(getTranslation("ucp.changeEmail.invalid"))
            return
        }

        val currentUser = ucpSessionService.getCurrentUser()
        when (emailChangeService.requestEmailChange(currentUser.userId, newEmail.value)) {
            EmailChangeRequestResult.ConfirmationSent -> {
                Dialog().apply {
                    add(H2(getTranslation("ucp.changeEmail.sent.title")))
                    add(Span(getTranslation("ucp.changeEmail.sent.details")))
                    footer.add(
                        Button(getTranslation("ucp.changeEmail.sent.close")) { close() }.apply {
                            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                        },
                    )
                    open()
                }
                newEmail.clear()
            }
            EmailChangeRequestResult.InvalidEmail -> showError(getTranslation("ucp.changeEmail.invalid"))
            EmailChangeRequestResult.BlacklistedEmail -> showError(getTranslation("ucp.changeEmail.blacklisted"))
            EmailChangeRequestResult.EmailAlreadyTaken -> showError(getTranslation("ucp.changeEmail.taken"))
            EmailChangeRequestResult.UnchangedEmail -> showError(getTranslation("ucp.changeEmail.unchanged"))
            EmailChangeRequestResult.UserNotFound -> showError(getTranslation("ucp.changeEmail.userNotFound"))
        }
    }

    private fun showError(message: String) {
        Notification.show(message, 5000, Notification.Position.MIDDLE).apply {
            addThemeVariants(NotificationVariant.LUMO_ERROR)
        }
    }
}
