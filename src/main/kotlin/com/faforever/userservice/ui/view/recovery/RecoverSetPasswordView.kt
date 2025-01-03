package com.faforever.userservice.ui.view.recovery

import com.faforever.userservice.backend.account.RecoveryService
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.faforever.userservice.ui.view.registration.ActivateView.PasswordConfirmation
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
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route

@Route("/recover-account/set-password", layout = CardLayout::class)
class RecoverSetPasswordView(
    private val recoveryService: RecoveryService,
) : CompactVerticalLayout(), BeforeEnterObserver {
    private val usernameInRecovery = HtmlComponent("small")

    private val password = PasswordField(null, getTranslation("register.password")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
    }
    private val confirmedPassword = PasswordField(null, getTranslation("register.password.confirm")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
    }

    private val submit =
        Button(getTranslation("recovery.setPassword.submit")) { setPassword() }.apply {
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            setWidthFull()
            isEnabled = false
        }

    private val binder = Binder(PasswordConfirmation::class.java)
    private var user: User? = null
    private var recoveryType = RecoveryService.Type.EMAIL

    init {
        maxWidth = "30rem"

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("recovery.setPassword.title"))
        val formHeader =
            HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
                justifyContentMode = FlexComponent.JustifyContentMode.CENTER
                alignItems = FlexComponent.Alignment.CENTER
                setId("form-header")
                setWidthFull()
            }

        add(formHeader)
        add(Paragraph(usernameInRecovery), password, confirmedPassword)
        add(submit)

        val footer =
            VerticalLayout(SocialIcons()).apply {
                alignItems = FlexComponent.Alignment.CENTER
            }

        add(footer)

        binder.forField(password)
            .asRequired(getTranslation("register.password.required"))
            .withValidator({ it.length >= 6 }, getTranslation("register.password.size"))
            .bind("password")

        binder.forField(confirmedPassword)
            .withValidator(
                { confirmedPassword -> confirmedPassword == password.value },
                getTranslation("register.password.match"),
            ).bind("confirmedPassword")

        binder.addStatusChangeListener { submit.isEnabled = it.binder.isValid }
    }

    private fun setPassword() {
        recoveryService.resetPassword(recoveryType, user?.id!!, password.value)

        Dialog().apply {
            add(H2(getTranslation("recovery.setPassword.confirmed.title")))
            add(Span(getTranslation("recovery.setPassword.confirmed.hint")))
            isCloseOnOutsideClick = false
            open()
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val parameters = event?.location?.queryParameters?.parameters ?: emptyMap()

        val (recoveryType, user) = try {
            recoveryService.parseRecoveryHttpRequest(parameters)
        } catch (e: Exception) {
            showDialog("recovery.setPassword.failed.title", "recovery.setPassword.invalidToken")
            return
        }

        if (user == null) {
            when (recoveryType) {
                RecoveryService.Type.EMAIL ->
                    showDialog("recovery.setPassword.failed.title", "recovery.setPassword.invalidToken")
                RecoveryService.Type.STEAM ->
                    showDialog("recovery.setPassword.failed.title", "recovery.steam.unknownUser")
            }
        } else {
            usernameInRecovery.element.text =
                getTranslation("recovery.setPassword.usernameInRecovery", user.username ?: "")
        }

        this.recoveryType = recoveryType
        this.user = user
    }

    private fun showDialog(titleKey: String, messageKey: String?) {
        Dialog().apply {
            add(H2(getTranslation(titleKey)))

            if (messageKey != null) {
                add(Span(getTranslation(messageKey)))
            }

            isCloseOnOutsideClick = false
            open()
        }
    }
}
