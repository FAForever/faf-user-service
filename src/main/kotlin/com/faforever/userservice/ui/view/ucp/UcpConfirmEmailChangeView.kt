package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.account.EmailChangeConfirmationResult
import com.faforever.userservice.backend.account.EmailChangeService
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed

@Route("/ucp/email/confirm", layout = CardLayout::class)
@AnonymousAllowed
class UcpConfirmEmailChangeView(
    private val emailChangeService: EmailChangeService,
) : CompactVerticalLayout(), BeforeEnterObserver {

    private val title = H2(getTranslation("ucp.changeEmail.confirm.title"))
    private val message = Paragraph()

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
            Button(getTranslation("ucp.changeEmail.confirm.login")) {
                ui.ifPresent { it.navigate(UcpLoginView::class.java) }
            }.apply {
                addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                setWidthFull()
            },
            VerticalLayout(SocialIcons()).apply {
                alignItems = FlexComponent.Alignment.CENTER
            },
        )
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val token = event.location.queryParameters.parameters["token"]?.firstOrNull()
        val result = if (token.isNullOrBlank()) {
            EmailChangeConfirmationResult.InvalidToken
        } else {
            emailChangeService.confirmEmailChange(token)
        }

        message.text = when (result) {
            EmailChangeConfirmationResult.Confirmed -> getTranslation("ucp.changeEmail.confirm.success")
            EmailChangeConfirmationResult.InvalidToken -> getTranslation("ucp.changeEmail.confirm.invalidToken")
            EmailChangeConfirmationResult.UserNotFound -> getTranslation("ucp.changeEmail.confirm.userNotFound")
            EmailChangeConfirmationResult.EmailUnavailable -> getTranslation("ucp.changeEmail.confirm.emailUnavailable")
        }
    }
}
