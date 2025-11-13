package com.faforever.userservice.ui.view.oauth2

import com.faforever.userservice.backend.account.LoginResult
import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.LoginResponse
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.backend.security.VaadinIpService
import com.faforever.userservice.backend.tos.TosService
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.ui.component.FontAwesomeIcon
import com.faforever.userservice.ui.component.LogoHeader
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.IFrame
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import java.net.URI
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle

@Route("/oauth2/login", layout = CardLayout::class)
class LoginView(
    private val hydraService: HydraService,
    private val vaadinIpService: VaadinIpService,
    private val fafProperties: FafProperties,
    private val tosService: TosService,
) :
    CompactVerticalLayout(), BeforeEnterObserver {

    private val footer = VerticalLayout().apply {
        val passwordReset =
            Anchor(fafProperties.account().passwordResetUrl(), getTranslation("login.forgotPassword"))
        val registerAccount =
            Anchor(fafProperties.account().registerAccountUrl(), getTranslation("login.registerAccount"))

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
        errorMessage.style.set("white-space", "pre-line")
        add(errorMessage)
    }

    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
        .append(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        .appendLiteral(" ")
        .append(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        .toFormatter(UI.getCurrent().locale)

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
        addClickShortcut(Key.ENTER)
    }
    private val loginLayout = CompactVerticalLayout(usernameOrEmail, password, submit).apply {
        width = "100%"
    }

    private lateinit var challenge: String

    init {
        add(header)
        add(errorLayout)
        add(loginLayout)
        add(footer)
    }

    fun login() {
        val ipAddress = vaadinIpService.getRealIp()
        when (val loginResponse = hydraService.login(challenge, usernameOrEmail.value, password.value, ipAddress)) {
            is LoginResponse.FailedLogin -> displayErrorMessage(loginResponse.recoverableLoginFailure)
            is LoginResponse.RejectedLogin -> displayRejectedMessage(loginResponse.unrecoverableLoginFailure)
            is LoginResponse.SuccessfulLogin -> onSuccessfulLogin(loginResponse)
        }
    }

    private fun onSuccessfulLogin(loginResponse: LoginResponse.SuccessfulLogin) {
        val userId = loginResponse.userId.toInt()
        val hasUserAcceptedLatestTos = tosService.hasUserAcceptedLatestTos(userId)
        val redirectUri = loginResponse.redirectTo.uri

        if (!hasUserAcceptedLatestTos) {
            showTosConsentModal(redirectUri, userId)
        } else {
            redirectToUrl(redirectUri)
        }
    }

    private fun showTosConsentModal(redirectUri: URI, userId: Int) {
        val modalDialog = Dialog().apply {
            headerTitle = getTranslation("login.tos.dialogTitle")
            width = "70%"
            height = "90%"
            isCloseOnEsc = false
            isCloseOnOutsideClick = false
        }

        val iframe = IFrame(fafProperties.account().registration().termsOfServiceUrl()).apply {
            width = "100%"
            height = "90%"
        }

        val acceptButton = Button(getTranslation("login.tos.dialogAcceptBtn")) {
            modalDialog.close()
            onTosAccept(userId, redirectUri)
        }.apply { addThemeVariants(ButtonVariant.LUMO_PRIMARY) }

        val declineButton = Button(getTranslation("login.tos.dialogDeclineBtn")) { onTosDecline() }
        val buttonLayout = HorizontalLayout(acceptButton, declineButton).apply {
            justifyContentMode = FlexComponent.JustifyContentMode.END
        }

        modalDialog.add(iframe, buttonLayout)
        modalDialog.open()
    }

    private fun onTosAccept(userId: Int, redirectUri: URI) {
        tosService.acceptLatestTos(userId)
        redirectToUrl(redirectUri)
    }

    private fun onTosDecline() {
        Dialog().apply {
            add(Span(getTranslation("login.tos.notAcceptedMsg")))
            open()
        }
    }

    private fun redirectToUrl(redirectUri: URI) {
        ui.ifPresent { it.page.setLocation(redirectUri) }
    }

    private fun displayErrorMessage(loginError: LoginResult.RecoverableLoginFailure) {
        errorMessage.text = when (loginError) {
            is LoginResult.RecoverableLoginOrCredentialsMismatch -> getTranslation("login.badCredentials")
            is LoginResult.ThrottlingActive -> getTranslation("login.throttled")
            is LoginResult.MissedBan -> {
                val startTime = loginError.startTime.format(dateTimeFormatter)
                val endTime = loginError.startTime.format(dateTimeFormatter)
                val intro = getTranslation("ban.missed.intro", startTime, endTime)
                val reason = "${getTranslation("ban.reason")} ${loginError.reason}"
                val explanation = getTranslation("ban.missed")
                "$intro\n$reason\n$explanation"
            }
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
                val expiration = loginError.expiresAt?.format(dateTimeFormatter) ?: getTranslation(
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
