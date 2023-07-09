package com.example.starter.base.ui.component

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.hydra.HydraService
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.*
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.server.VaadinSession
import jakarta.enterprise.context.Dependent

class LogoHeader(title: String) : HorizontalLayout() {
    init {
        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(title)
        add(formHeaderLeft, formHeaderRight)
        alignItems = FlexComponent.Alignment.CENTER
        setId("form-header")
    }
}

abstract class Card : VerticalLayout() {

    protected val innerCard = VerticalLayout()

    init {
        addClassName("main-card")
        innerCard.addClassName("main-card-inner")
        add(innerCard)
    }
}

@Dependent
class LoginCard(private val hydraService: HydraService) : Card(), BeforeEnterObserver {
    private val errorLayout = VerticalLayout()
    private val logoHeader = LogoHeader("Welcome back, Commander!")
    private val message = Span()

    private val usernameOrEmail = TextField(null, "Username or Email")
    private val password = PasswordField(null, "Password")
    private val submit = Button("Log in") {login()}

    private lateinit var challenge: String

    init {
        val layout = VerticalLayout()

        layout.add(logoHeader)

        errorLayout.addClassName("error")
        errorLayout.add(message)
        errorLayout.isVisible = false

        layout.add(errorLayout)

        usernameOrEmail.setWidthFull()
        password.setWidthFull()
        submit.setWidthFull()

        layout.add(usernameOrEmail, password, submit)

        val footer = HorizontalLayout()

        val links = UnorderedList()
        links.addClassName("pipe-separated")

        val passwordReset = ListItem(Anchor("https://faforever.com/account/password/reset", "Forgot Password"))
        val registerAccount = ListItem(Anchor("https://faforever.com/account/register", "Register Account"))

        links.add(passwordReset, registerAccount)
        footer.add(links)
        footer.justifyContentMode = FlexComponent.JustifyContentMode.CENTER

        layout.add(footer)

        innerCard.add(layout)
    }

    fun login() {
        val ipAddress = IpAddress(VaadinSession.getCurrent().browser.address);
        val (redirectTo, loginResult) = hydraService.login(challenge, usernameOrEmail.value, password.value, ipAddress)
        ui.ifPresent { it.page.setLocation(redirectTo.uri) }
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val possibleChallenge = event?.location?.queryParameters?.parameters?.get("login_challenge")?.get(0)
        if (possibleChallenge != null) {
            challenge = possibleChallenge
        }
    }

}