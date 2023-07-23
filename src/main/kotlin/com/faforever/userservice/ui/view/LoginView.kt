package com.faforever.userservice.ui.view

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.LoginResult
import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.LoginResponse
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.backend.i18n.I18n
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.FontAwesomeIcon
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.faforever.userservice.ui.layout.OAuthCardLayout
import com.vaadin.flow.component.Key
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

@Route("/oauth2/login", layout = OAuthCardLayout::class)
class LoginView(private val hydraService: HydraService, private val i18n: I18n) : CompactVerticalLayout(),
    BeforeEnterObserver {

    private val errorLayout = HorizontalLayout()
    private val errorMessage = Span()

    private val usernameOrEmail = TextField(null, i18n.getTranslation("login.usernameOrEmail"))
    private val password = PasswordField(null, i18n.getTranslation("login.password"))
    private val submit = Button(i18n.getTranslation("login.loginAction")) { login() }

    private lateinit var challenge: String

    init {
        val formHeader = HorizontalLayout()

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(i18n.getTranslation("login.welcomeBack"))
        formHeader.add(formHeaderLeft, formHeaderRight)
        formHeader.alignItems = FlexComponent.Alignment.CENTER
        formHeader.setId("form-header")
        formHeader.setWidthFull()

        add(formHeader)

        errorLayout.setWidthFull()
        errorLayout.addClassName("error")
        val errorIcon = FontAwesomeIcon()
        errorIcon.addClassNames("fas fa-exclamation-triangle")
        errorLayout.add(errorIcon)
        errorLayout.add(errorMessage)
        errorLayout.isVisible = false
        errorLayout.alignItems = FlexComponent.Alignment.CENTER
        errorLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)

        add(errorLayout)

        password.addKeyUpListener {
            if (it.key.equals(Key.ENTER)) {
                login()
            }
        }

        usernameOrEmail.setWidthFull()
        password.setWidthFull()
        submit.setWidthFull()
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        add(usernameOrEmail, password, submit)

        val footer = VerticalLayout()

        val links = HorizontalLayout()
        links.addClassName("pipe-separated")

        val passwordReset =
            Anchor("https://faforever.com/account/password/reset", i18n.getTranslation("login.forgotPassword"))
        val registerAccount =
            Anchor("https://faforever.com/account/register", i18n.getTranslation("login.registerAccount"))

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
            is LoginResult.UserOrCredentialsMismatch -> i18n.getTranslation("login.badCredentials")
            is LoginResult.ThrottlingActive -> i18n.getTranslation("login.throttled")
        }
        errorLayout.isVisible = true
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val possibleChallenge = event?.location?.queryParameters?.parameters?.get("login_challenge")?.get(0)
        if (possibleChallenge != null) {
            challenge = possibleChallenge
            hydraService.getLoginRequest(challenge)
        } else {
            throw NoChallengeException()
        }
    }
}