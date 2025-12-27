package com.faforever.userservice.ui.view.recovery

import com.faforever.userservice.backend.account.RecoveryService
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.HtmlComponent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route

@Route("/recover-account/email", layout = CardLayout::class)
class RecoverViaEmailView(
    private val recoveryService: RecoveryService,
) : CompactVerticalLayout(), BeforeEnterObserver {

    private val usernameOrEmailDescription =
        Paragraph(
            HtmlComponent("small").apply {
                element.setProperty(
                    "innerHTML",
                    getTranslation("recovery.email.usernameOrEmailAlternative", "/recover-account"),
                )
            },
        )

    private val usernameOrEmail =
        TextField(null, getTranslation("recovery.email.usernameOrEmail")).apply {
            setWidthFull()
            valueChangeMode = ValueChangeMode.LAZY
        }

    private val submit =
        Button(getTranslation("recovery.email.submit")) { requestEmail() }.apply {
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            setWidthFull()
        }

    init {
        maxWidth = "30rem"

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("recovery.email.title"))
        val formHeader =
            HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
                justifyContentMode = FlexComponent.JustifyContentMode.CENTER
                alignItems = FlexComponent.Alignment.CENTER
                setId("form-header")
                setWidthFull()
            }

        add(formHeader)
        add(usernameOrEmail, usernameOrEmailDescription)
        add(submit)

        val footer =
            VerticalLayout(SocialIcons()).apply {
                alignItems = FlexComponent.Alignment.CENTER
            }

        add(footer)
    }

    private fun requestEmail() {
        recoveryService.requestPasswordResetViaEmail(usernameOrEmail.value)

        Dialog().apply {
            add(H2(getTranslation("recovery.email.sent.title")))
            add(Span(getTranslation("recovery.email.sent.hint")))
            isCloseOnOutsideClick = false
            open()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val possibleIdentifier = event?.location?.queryParameters?.parameters?.get("identifier")?.get(0)
        if (!possibleIdentifier.isNullOrBlank()) {
            usernameOrEmail.value = possibleIdentifier
        }
    }
}
