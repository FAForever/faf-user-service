package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.account.LoginResult
import com.faforever.userservice.backend.account.LoginService
import com.faforever.userservice.backend.security.VaadinIpService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.backend.ucp.UcpUser
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.login.LoginI18n
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.auth.AnonymousAllowed

@Route("/ucp/login", layout = CardLayout::class)
@AnonymousAllowed
class UcpLoginView(
    private val loginService: LoginService,
    private val vaadinIpService: VaadinIpService,
    private val ucpSessionService: UcpSessionService,
    private val fafProperties: FafProperties,
) : CompactVerticalLayout(),
    BeforeEnterObserver {

    private val loginI18n = LoginI18n().apply {
        form = LoginI18n.Form().apply {
            title = getTranslation("ucp.login.heading")
            submit = getTranslation("login.loginAction")
            username = getTranslation("login.usernameOrEmail")
            password = getTranslation("login.password")
            forgotPassword = getTranslation("login.forgotPassword")
        }
    }

    private val loginForm = LoginForm().apply {
        setI18n(loginI18n)
        setWidthFull()
        addLoginListener { e ->
            val result = loginService.loginForUcp(
                e.username,
                e.password,
                vaadinIpService.getRealIp(),
            )
            when (result) {
                is LoginResult.SuccessfulLogin -> {
                    isError = false
                    VaadinRequest.getCurrent()?.let { VaadinService.reinitializeSession(it) }
                    ucpSessionService.setCurrentUser(UcpUser(result.userId, result.userName))
                    UI.getCurrent().navigate(UcpAccountDataView::class.java)
                }

                is LoginResult.ThrottlingActive -> {
                    isError = true
                    setI18n(loginI18n.apply { errorMessage = LoginI18n.ErrorMessage().apply {
                        title = getTranslation("login.throttled")
                        message = ""
                    } })
                }

                is LoginResult.RecoverableLoginOrCredentialsMismatch -> {
                    isError = true
                    setI18n(loginI18n.apply { errorMessage = LoginI18n.ErrorMessage().apply {
                        title = getTranslation("login.badCredentials")
                        message = ""
                    } })
                }

                else -> {
                    isError = true
                    setI18n(loginI18n.apply { errorMessage = LoginI18n.ErrorMessage().apply {
                        title = getTranslation("login.technicalError")
                        message = ""
                    } })
                }
            }
        }
        addForgotPasswordListener { e ->
            e.source.ui.ifPresent { ui ->
                ui.navigate(fafProperties.account().passwordResetUrl())
            }
        }
    }

    init {
        maxWidth = "30rem"
        alignItems = FlexComponent.Alignment.STRETCH

        add(loginForm)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (ucpSessionService.isLoggedIn()) {
            event.forwardTo(UcpAccountDataView::class.java)
        }
    }
}
