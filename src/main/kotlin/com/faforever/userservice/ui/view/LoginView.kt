package com.faforever.userservice.ui.view

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.LoginResult
import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.LoginResponse
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.ui.component.FontAwesomeIcon
import com.faforever.userservice.ui.component.LogoHeader
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.faforever.userservice.ui.layout.OAuthCardLayout
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Anchor
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
import java.time.format.DateTimeFormatter

@Route("/oauth2/login", layout = OAuthCardLayout::class)
class LoginView(private val hydraService: HydraService, private val fafProperties: FafProperties) :
    CompactVerticalLayout(), BeforeEnterObserver {

    private val loginLayout = CompactVerticalLayout()
    private val footer = VerticalLayout()
    private val header = LogoHeader()

    private val errorLayout = HorizontalLayout()
    private val errorMessage = Span()

    private val usernameOrEmail = TextField(null, getTranslation("login.usernameOrEmail"))
    private val password = PasswordField(null, getTranslation("login.password"))

    private val submit = Button(getTranslation("login.loginAction")) { login() }

    private lateinit var challenge: String

    init {
        header.setTitle(getTranslation("login.welcomeBack"))
        add(header)

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

        loginLayout.add(usernameOrEmail, password, submit)
        add(loginLayout)

        val footer = footer

        val links = HorizontalLayout()
        links.addClassName("pipe-separated")

        val resetHref = "https://faforever.com/account/password/reset"
        val passwordReset = Anchor(resetHref, getTranslation("login.forgotPassword"))
        val registerHref = "https://faforever.com/account/register"
        val registerAccount = Anchor(registerHref, getTranslation("login.registerAccount"))

        links.add(passwordReset, registerAccount)
        footer.add(links)
        footer.add(SocialIcons())
        footer.alignItems = FlexComponent.Alignment.CENTER

        add(footer)
    }

    fun login() {
        val ipAddress = IpAddress(VaadinSession.getCurrent().browser.address)
        when (val loginResponse = hydraService.login(challenge, usernameOrEmail.value, password.value, ipAddress)) {
            is LoginResponse.FailedLogin -> displayErrorMessage(loginResponse.recoverableLoginFailure)
            is LoginResponse.RejectedLogin -> displayRejectedMessage(loginResponse.unrecoverableLoginFailure)
            is LoginResponse.SuccessfulLogin -> ui.ifPresent { it.page.setLocation(loginResponse.redirectTo.uri) }
        }
    }

    private fun displayErrorMessage(loginError: LoginResult.RecoverableLoginFailure) {
        errorMessage.text = when (loginError) {
            is LoginResult.RecoverableLoginOrCredentialsMismatch -> getTranslation("login.badCredentials")
            is LoginResult.ThrottlingActive -> getTranslation("login.throttled")
        }
        errorLayout.isVisible = true
    }

    private fun displayRejectedMessage(loginError: LoginResult.UnrecoverableLoginFailure) {
        loginLayout.isVisible = false
        footer.isVisible = false
        errorLayout.isVisible = true

        when (loginError) {
            is LoginResult.UserNoGameOwnership -> {
                header.setTitle(getTranslation("verification.title"))
                errorMessage.text = getTranslation("verification.reason") + " " + fafProperties.accountLinkUrl()
            }

            is LoginResult.TechnicalError -> {
                header.setTitle(getTranslation("title.technicalError"))
                errorMessage.text = getTranslation("login.technicalError")
            }

            is LoginResult.UserBanned -> {
                header.setTitle(getTranslation("ban.title"))
                val expiration = loginError.expiresAt?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) ?: getTranslation(
                    "ban.permanent",
                )
                val expirationText = "${getTranslation("ban.expiration")} $expiration."
                val reason = "${getTranslation("ban.reason")} ${loginError.reason}"
                errorMessage.text = "$expirationText $reason"
            }
        }
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
