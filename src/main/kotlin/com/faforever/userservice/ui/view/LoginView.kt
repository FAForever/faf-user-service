package com.faforever.userservice.ui.view

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.LoginResult
import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.LoginResponse
import com.faforever.userservice.ui.component.CompactVerticalLayout
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.Icon
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinSession

@Route("/oauth2/login", layout = CardLayout::class)
class LoginView(private val hydraService: HydraService) : CompactVerticalLayout(), BeforeEnterObserver {

    private val errorLayout = HorizontalLayout()
    private val errorMessage = Span()

    private val usernameOrEmail = TextField(null, "Username or Email")
    private val password = PasswordField(null, "Password")
    private val submit = Button("Log in") {login()}

    private lateinit var challenge: String

    init {
        val formHeader = HorizontalLayout()

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2("Welcome back, Commander!")
        formHeader.add(formHeaderLeft, formHeaderRight)
        formHeader.alignItems = FlexComponent.Alignment.CENTER
        formHeader.setId("form-header")

        add(formHeader)

        errorLayout.addClassName("error")
        val errorIcon = Icon()
        errorIcon.addClassNames("fas fa-exclamation-triangle")
        errorLayout.add(errorIcon)
        errorLayout.add(errorMessage)
        errorLayout.isVisible = false
        errorLayout.alignItems = FlexComponent.Alignment.CENTER
        errorLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)

        add(errorLayout)

        usernameOrEmail.setWidthFull()
        password.setWidthFull()
        submit.setWidthFull()
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        add(usernameOrEmail, password, submit)

        val footer = VerticalLayout()

        val links = HorizontalLayout()
        links.addClassName("pipe-separated")

        val passwordReset = Anchor("https://faforever.com/account/password/reset", "Forgot Password")
        val registerAccount = Anchor("https://faforever.com/account/register", "Register Account")

        links.add(passwordReset, registerAccount)
        footer.add(links)
        footer.add(SocialIcons())
        footer.alignItems = FlexComponent.Alignment.CENTER

        add(footer)
    }

    fun login() {
        val ipAddress = IpAddress(VaadinSession.getCurrent().browser.address);
        when (val loginResponse = hydraService.login(challenge, usernameOrEmail.value, password.value, ipAddress)) {
            is LoginResponse.FailedLogin -> setError(loginResponse.userError)
            is LoginResponse.RejectedLogin -> ui.ifPresent { it.page.setLocation(loginResponse.redirectTo.uri) }
            is LoginResponse.SuccessfulLogin -> ui.ifPresent { it.page.setLocation(loginResponse.redirectTo.uri) }
        }
    }

    private fun setError(loginError: LoginResult.UserError) {
        errorMessage.text = when (loginError) {
            is LoginResult.UserOrCredentialsMismatch -> "Username or password does not match"
            is LoginResult.ThrottlingActive -> "Too many of your login attempts have failed. Please wait some time before trying to login again"
        }
        errorLayout.isVisible = true
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val possibleChallenge = event?.location?.queryParameters?.parameters?.get("login_challenge")?.get(0)
        if (possibleChallenge != null) {
            challenge = possibleChallenge
        }
    }
}