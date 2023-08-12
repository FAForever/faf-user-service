package com.faforever.userservice.ui.view.oauth2

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.LoginResponse
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.backend.login.LoginResult
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.ui.component.FontAwesomeIcon
import com.faforever.userservice.ui.component.LogoHeader
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
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
import com.vaadin.flow.server.VaadinRequest
import java.time.format.DateTimeFormatter

@Route("/oauth2/login", layout = CardLayout::class)
class LoginView(private val hydraService: HydraService, private val fafProperties: FafProperties) :
    CompactVerticalLayout(), BeforeEnterObserver {

    private val footer = VerticalLayout().apply {
        val resetHref = "https://faforever.com/account/password/reset"
        val passwordReset = Anchor(resetHref, getTranslation("login.forgotPassword"))
        val registerHref = "https://faforever.com/account/register"
        val registerAccount = Anchor(registerHref, getTranslation("login.registerAccount"))

        val links = HorizontalLayout(passwordReset, registerAccount).apply {
            addClassNames("pipe-separated")
        }

        add(links)
        add(SocialIcons())
        alignItems = FlexComponent.Alignment.CENTER
    }
    private val header = LogoHeader().apply {
        setTitle(getTranslation("login.welcomeBack"))
    }

    private val errorMessage = Span()
    private val errorLayout = HorizontalLayout().apply {
        isVisible = false
        alignItems = FlexComponent.Alignment.CENTER
        setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)
        setWidthFull()
        addClassName("error")
        add(FontAwesomeIcon().apply { addClassNames("fas fa-exclamation-triangle") })
        add(errorMessage)
    }

    private val usernameOrEmail = TextField(null, getTranslation("login.usernameOrEmail")).apply {
        setWidthFull()
    }
    private val password = PasswordField(null, getTranslation("login.password")).apply {
        setWidthFull()
        addKeyUpListener {
            if (it.key.equals(Key.ENTER)) {
                login()
            }
        }
    }

    private val submit = Button(getTranslation("login.loginAction")) { login() }.apply {
        setWidthFull()
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
    }
    private val loginLayout = CompactVerticalLayout(usernameOrEmail, password, submit)

    private lateinit var challenge: String

    init {
        add(header)
        add(errorLayout)
        add(loginLayout)
        add(footer)
    }

    fun login() {
        val currentRequest = VaadinRequest.getCurrent()
        val realIp = currentRequest.getHeader(fafProperties.realIpHeader()) ?: currentRequest.remoteAddr
        val ipAddress = IpAddress(realIp)
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
                errorMessage.text = getTranslation("verification.reason") + " " +
                    fafProperties.account().accountLinkUrl()
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
