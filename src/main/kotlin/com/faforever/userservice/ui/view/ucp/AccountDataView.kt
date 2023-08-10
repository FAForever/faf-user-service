package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.i18n.I18n
import com.faforever.userservice.backend.security.CurrentUserService
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.security.PasswordService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Route("/ucp")
class AccountDataView(
    private val i18n: I18n,
    private val currentUserService: CurrentUserService,
    private val emailService: EmailService,
    private val passwordService: PasswordService,
) : UcpLayout(i18n), BeforeEnterObserver {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FafTokenService::class.java)
    }

    private val usernameField = TextField().apply {
        label = "Username"
        prefixComponent = VaadinIcon.USER.create()
    }

    private val usernameChangeButton = Button().apply {
        text = "Change username"
        prefixComponent = VaadinIcon.EDIT.create()
        addClickListener {
            changeUsername(usernameField.value)
        }
    }

    private val emailField = TextField().apply {
        label = "Email"
        prefixComponent = VaadinIcon.ENVELOPE_O.create()
    }

    private val emailChangeButton = Button().apply {
        text = "Change Email"
        prefixComponent = VaadinIcon.EDIT.create()
        addClickListener {
            changeEmail(emailField.value)
        }
    }

    private val passwordField = PasswordField().apply {
        label = "Password"
        prefixComponent = VaadinIcon.PASSWORD.create()
    }

    private val passwordChangeButton = Button().apply {
        text = "Change Password"
        prefixComponent = VaadinIcon.EDIT.create()
        addClickListener {
            changePassword("", passwordField.value)
        }
    }

    init {
        content = Div().apply {
            add(H2("Account Data"))

            add(
                Div().apply {
                    add(usernameField)
                    add(usernameChangeButton)
                },
            )

            add(
                Div().apply {
                    add(emailField)
                    add(emailChangeButton)
                },
            )

            add(
                Div().apply {
                    add(passwordField)
                    add(passwordChangeButton)
                },
            )
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = currentUserService.requireUser()

        usernameField.value = user.username
        emailField.value = user.email
    }

    private fun changeUsername(username: String) {
        if (username.length > 20) {
            usernameField.isInvalid = true
            usernameField.errorMessage = "Username is too long"

            return
        }

        usernameField.isInvalid = false
        usernameField.value = username
    }

    private fun changeEmail(email: String) {
        when (emailService.validateEmailAddress(email)) {
            EmailService.ValidationResult.INVALID -> {
                log.debug("Mail address $email invalid")
                emailField.isInvalid = true
                emailField.errorMessage = "Mail address invalid"
                return
            }
            EmailService.ValidationResult.BLACKLISTED -> {
                log.debug("Email provider of $email is blacklisted")
                emailField.isInvalid = true
                emailField.errorMessage = "Email provider is blacklisted"
                return
            }
            EmailService.ValidationResult.VALID -> {
                val currentUser = currentUserService.requireUser()
                log.info("Email address of user ${currentUser.id} changed from ${currentUser.email} to $email")
                emailService.changeUserEmail(email, currentUserService.requireUser())
                emailField.isInvalid = false
                emailField.errorMessage = null
                emailField.value = email
            }
        }
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        when (passwordService.validatePassword(newPassword)) {
            PasswordService.ValidatePasswordResult.TOO_SHORT -> {
                log.debug("Password is too short")
                passwordField.isInvalid = true
                passwordField.errorMessage = "Password is too short"
                return
            }
            PasswordService.ValidatePasswordResult.VALID -> log.debug("Password is valid")
        }

        when (passwordService.changePassword(currentUserService.requireUser(), oldPassword, newPassword)) {
            PasswordService.ChangePasswordResult.PASSWORD_MISMATCH -> {
                log.debug("Old password did not match")
                passwordField.isInvalid = true
                passwordField.errorMessage = "Old password did not match"
            }
            PasswordService.ChangePasswordResult.OK -> {
                log.debug("Password was changed")
                currentUserService.invalidate()
                passwordField.isInvalid = false
                passwordField.errorMessage = null
                passwordField.clear()
            }
        }
    }
}
