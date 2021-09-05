package com.faforever.userservice.ui.view.registration

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.registration.InvalidRegistrationException
import com.faforever.userservice.backend.registration.RegisteredUser
import com.faforever.userservice.backend.registration.RegistrationService
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinSession

@Route("/register/activate", layout = CardLayout::class)
class ActivateView(private val registrationService: RegistrationService, fafProperties: FafProperties) :
    CompactVerticalLayout(), BeforeEnterObserver {

    companion object {
        class PasswordConfirmation {
            var password: String = ""
            var confirmedPassword: String = ""
        }
    }

    private val username = TextField(null, getTranslation("register.username"))
    private val email = TextField(null, getTranslation("register.email"))
    private val password = PasswordField(null, getTranslation("register.password"))
    private val confirmedPassword = PasswordField(null, getTranslation("register.password.confirm"))
    private val submit = Button(getTranslation("register.activate")) { activate() }

    private val binder = Binder(PasswordConfirmation::class.java)

    private lateinit var registeredUser: RegisteredUser

    init {
        val formHeader = HorizontalLayout()

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("register.activate"))
        formHeader.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        formHeader.add(formHeaderLeft, formHeaderRight)
        formHeader.alignItems = FlexComponent.Alignment.CENTER
        formHeader.setId("form-header")
        formHeader.setWidthFull()

        add(formHeader)

        username.setWidthFull()
        username.isReadOnly = true

        email.setWidthFull()
        email.isReadOnly = true

        password.setWidthFull()
        confirmedPassword.setWidthFull()
        submit.isEnabled = false
        submit.setWidthFull()
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        add(username, email, password, confirmedPassword, submit)

        val footer = VerticalLayout()
        footer.add(SocialIcons())
        footer.alignItems = FlexComponent.Alignment.CENTER

        add(footer)

        binder.forField(password)
            .asRequired(getTranslation("register.password.required"))
            .withValidator({ username -> username.length >= 6 }, getTranslation("register.password.size"))
            .bind("password")

        binder.forField(confirmedPassword)
            .withValidator({ confirmedPassword -> confirmedPassword == password.value }, getTranslation("register.password.match")
            ).bind("confirmedPassword")

        binder.addStatusChangeListener { submit.isEnabled = it.binder.isValid }

    }

    private fun activate() {
        val validationStatus = binder.validate()
        if (validationStatus.hasErrors()) {
            return
        }

        val ipAddress = IpAddress(VaadinSession.getCurrent().browser.address);

        registrationService.activate(registeredUser, ipAddress, password.value)

        val successDialog = Dialog()
        successDialog.add(H2(getTranslation("register.activated")))
        successDialog.add(Span(getTranslation("register.activated.details")))
        successDialog.open()

        binder.readBean(null)
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val possibleToken = event?.location?.queryParameters?.parameters?.get("token")?.get(0)
        if (possibleToken != null) {
            registeredUser = registrationService.validateRegistrationToken(possibleToken)
            username.value = registeredUser.username
            email.value = registeredUser.email
        } else {
            throw InvalidRegistrationException()
        }
    }
}