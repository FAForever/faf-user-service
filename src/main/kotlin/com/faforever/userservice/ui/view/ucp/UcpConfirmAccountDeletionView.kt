package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.account.AccountDeletionConfirmationResult
import com.faforever.userservice.backend.account.AccountDeletionService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed

@Route("/ucp/delete-account/confirm", layout = CardLayout::class)
@AnonymousAllowed
class UcpConfirmAccountDeletionView(
    private val accountDeletionService: AccountDeletionService,
    private val ucpSessionService: UcpSessionService,
) : CompactVerticalLayout(), BeforeEnterObserver {

    private val title = H2(getTranslation("ucp.deleteAccount.confirm.title"))
    private val message = Paragraph()
    private val finalWarning = Paragraph(getTranslation("ucp.deleteAccount.confirm.warning"))
    private val acknowledgement = Checkbox(getTranslation("ucp.deleteAccount.confirm.acknowledgement")).apply {
        setWidthFull()
    }
    private val confirmButton = Button(getTranslation("ucp.deleteAccount.confirm.submit")) {
        confirmAccountDeletion()
    }.apply {
        addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY)
        setWidthFull()
    }
    private val loginButton = Button(getTranslation("ucp.deleteAccount.confirm.login")) {
        ui.ifPresent { it.navigate(UcpLoginView::class.java) }
    }.apply {
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        setWidthFull()
    }

    private var token: String? = null

    init {
        maxWidth = "30rem"

        val formHeader = HorizontalLayout(FafLogo(), title).apply {
            justifyContentMode = FlexComponent.JustifyContentMode.CENTER
            alignItems = FlexComponent.Alignment.CENTER
            setId("form-header")
            setWidthFull()
        }

        add(
            formHeader,
            message,
            finalWarning,
            acknowledgement,
            confirmButton,
            loginButton,
            VerticalLayout(SocialIcons()).apply {
                alignItems = FlexComponent.Alignment.CENTER
            },
        )
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        token = event.location.queryParameters.parameters["token"]?.firstOrNull()

        if (token.isNullOrBlank()) {
            showInvalidResult(getTranslation("ucp.deleteAccount.confirm.invalidToken"))
        } else {
            showConfirmation()
        }
    }

    private fun showConfirmation() {
        message.text = getTranslation("ucp.deleteAccount.confirm.message")
        finalWarning.isVisible = true
        acknowledgement.isVisible = true
        confirmButton.isVisible = true
        loginButton.isVisible = false
    }

    private fun showInvalidResult(text: String) {
        message.text = text
        finalWarning.isVisible = false
        acknowledgement.isVisible = false
        confirmButton.isVisible = false
        loginButton.isVisible = true
    }

    private fun confirmAccountDeletion() {
        if (!acknowledgement.value) {
            Notification.show(
                getTranslation("ucp.deleteAccount.confirm.mustAcknowledge"),
                5000,
                Notification.Position.MIDDLE,
            ).apply {
                addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            return
        }

        val token = token
        val result = if (token.isNullOrBlank()) {
            AccountDeletionConfirmationResult.InvalidToken
        } else {
            accountDeletionService.confirmAccountDeletion(token)
        }

        message.text = when (result) {
            AccountDeletionConfirmationResult.Confirmed -> {
                ucpSessionService.logout()
                ui.ifPresent { it.page.setLocation("/ucp/login") }
                return
            }
            AccountDeletionConfirmationResult.InvalidToken -> getTranslation("ucp.deleteAccount.confirm.invalidToken")
            AccountDeletionConfirmationResult.UserNotFound -> getTranslation("ucp.deleteAccount.confirm.userNotFound")
            AccountDeletionConfirmationResult.AnonymizationFailed -> getTranslation("ucp.deleteAccount.confirm.failed")
        }

        finalWarning.isVisible = false
        acknowledgement.isVisible = false
        confirmButton.isVisible = false
        loginButton.isVisible = true
    }
}
