package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.account.PasswordChangeResult
import com.faforever.userservice.backend.account.PasswordChangeService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.faforever.userservice.ui.view.registration.ActivateView.PasswordConfirmation
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/password", layout = UcpLayout::class)
@PermitAll
class UcpChangePasswordView(
    private val ucpSessionService: UcpSessionService,
    private val passwordChangeService: PasswordChangeService,
) : VerticalLayout() {

    companion object {
        private const val NOTIFICATION_DURATION_MS = 5000
    }

    private val currentPassword = PasswordField(getTranslation("ucp.changePassword.currentPassword")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
        isRequiredIndicatorVisible = true
    }

    private val newPassword = PasswordField(getTranslation("ucp.changePassword.newPassword")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
        isRequiredIndicatorVisible = true
    }

    private val confirmPassword = PasswordField(getTranslation("ucp.changePassword.confirmPassword")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
        isRequiredIndicatorVisible = true
    }

    private val submit = Button(getTranslation("ucp.changePassword.submit")) { changePassword() }.apply {
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        isEnabled = false
    }

    private val binder = Binder(PasswordConfirmation::class.java)

    init {
        setPadding(true)
        setSizeFull()
        add(
            H2(getTranslation("ucp.nav.changePassword")),
            Paragraph(getTranslation("ucp.changePassword.description")),
            currentPassword,
            newPassword,
            confirmPassword,
            submit,
        )

        binder.forField(newPassword)
            .asRequired(getTranslation("ucp.changePassword.newPasswordRequired"))
            .withValidator(
                { it.length >= 6 },
                getTranslation("ucp.changePassword.newPasswordTooShort"),
            ).bind("password")

        binder.forField(confirmPassword)
            .withValidator(
                { it == newPassword.value },
                getTranslation("ucp.changePassword.passwordsDoNotMatch"),
            ).bind("confirmedPassword")

        binder.addStatusChangeListener { submit.isEnabled = it.binder.isValid }

        // Re-validate confirmPassword when newPassword changes
        newPassword.addValueChangeListener {
            binder.validate()
        }
    }

    private fun changePassword() {
        val currentUser = ucpSessionService.getCurrentUser()
        when {
            currentPassword.value.isNullOrBlank() ->
                showError(getTranslation("ucp.changePassword.currentPasswordRequired"))
            else -> {
                val result = passwordChangeService.changePassword(
                    currentUser.userId,
                    currentPassword.value,
                    newPassword.value,
                )
                when (result) {
                    PasswordChangeResult.Success -> {
                        clearFields()
                        Dialog().apply {
                            add(H2(getTranslation("ucp.changePassword.success.title")))
                            add(Span(getTranslation("ucp.changePassword.success.details")))
                            footer.add(
                                Button(getTranslation("ucp.changePassword.success.close")) { close() }.apply {
                                    addThemeVariants(ButtonVariant.LUMO_PRIMARY)
                                },
                            )
                            open()
                        }
                    }
                    PasswordChangeResult.InvalidCurrentPassword ->
                        showError(getTranslation("ucp.changePassword.currentPasswordIncorrect"))
                    PasswordChangeResult.PasswordUnchanged ->
                        showError(getTranslation("ucp.changePassword.passwordUnchanged"))
                }
            }
        }
    }

    private fun clearFields() {
        currentPassword.clear()
        newPassword.clear()
        confirmPassword.clear()

        // Reset binder state
        binder.readBean(null)
    }

    private fun showError(message: String) {
        Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.MIDDLE).apply {
            addThemeVariants(NotificationVariant.LUMO_ERROR)
        }
    }
}
