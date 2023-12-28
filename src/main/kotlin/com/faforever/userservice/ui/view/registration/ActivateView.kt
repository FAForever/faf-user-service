package com.faforever.userservice.ui.view.registration

import com.faforever.userservice.backend.registration.InvalidRegistrationException
import com.faforever.userservice.backend.registration.RegisteredUser
import com.faforever.userservice.backend.registration.RegistrationService
import com.faforever.userservice.backend.security.VaadinIpService
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
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route

@Route("/register/activate", layout = CardLayout::class)
class ActivateView(private val registrationService: RegistrationService, private val vaadinIpService: VaadinIpService) :
    CompactVerticalLayout(), BeforeEnterObserver {

    class PasswordConfirmation {
        var password: String = ""
        var confirmedPassword: String = ""
    }

    private val username = TextField(null, getTranslation("register.username")).apply {
        isReadOnly = true
        setWidthFull()
    }
    private val email = TextField(null, getTranslation("register.email")).apply {
        isReadOnly = true
        setWidthFull()
    }
    private val password = PasswordField(null, getTranslation("register.password")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
    }
    private val confirmedPassword = PasswordField(null, getTranslation("register.password.confirm")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
    }
    private val submit = Button(getTranslation("register.activate")) { activate() }.apply {
        isEnabled = false
        setWidthFull()
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
    }

    private val binder = Binder(PasswordConfirmation::class.java)

    private lateinit var registeredUser: RegisteredUser

    init {

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("register.activate"))
        val formHeader = HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
            justifyContentMode = FlexComponent.JustifyContentMode.CENTER
            alignItems = FlexComponent.Alignment.CENTER
            setId("form-header")
            setWidthFull()
        }

        add(formHeader)

        add(username, email, password, confirmedPassword, submit)

        val footer = VerticalLayout(SocialIcons()).apply {
            alignItems = FlexComponent.Alignment.CENTER
        }

        add(footer)

        binder.forField(password)
            .asRequired(getTranslation("register.password.required"))
            .withValidator({ username -> username.length >= 6 }, getTranslation("register.password.size"))
            .bind("password")

        binder.forField(confirmedPassword)
            .withValidator(
                { confirmedPassword -> confirmedPassword == password.value },
                getTranslation("register.password.match"),
            ).bind("confirmedPassword")

        binder.addStatusChangeListener { submit.isEnabled = it.binder.isValid }
    }

    private fun activate() {
        val validationStatus = binder.validate()
        if (validationStatus.hasErrors()) {
            return
        }

        val ipAddress = vaadinIpService.getRealIp()

        registrationService.activate(registeredUser, ipAddress, password.value)

        Dialog().apply {
            add(H2(getTranslation("register.activated")))
            add(Span(getTranslation("register.activated.details")))
            isCloseOnOutsideClick = false
            open()
        }

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
