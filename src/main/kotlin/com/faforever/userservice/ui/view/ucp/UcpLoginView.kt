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
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route

@Route("/ucp/login", layout = CardLayout::class)
class UcpLoginView(
    private val loginService: LoginService,
    private val vaadinIpService: VaadinIpService,
    private val ucpSessionService: UcpSessionService,
    private val fafProperties: FafProperties,
) : CompactVerticalLayout(),
    BeforeEnterObserver {

    private val loginForm = LoginForm().apply {
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
                    ucpSessionService.setCurrentUser(UcpUser(result.userId, result.userName))
                    UI.getCurrent().navigate(UcpAccountDataView::class.java)
                }
                is LoginResult.ThrottlingActive -> {
                    isError = false
                    Notification.show(getTranslation("ucp.login.throttled"), 5000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_WARNING)
                }
                is LoginResult.RecoverableLoginOrCredentialsMismatch -> {
                    isError = true
                }
                else -> {
                    isError = true
                }
            }
        }
    }

    init {
        maxWidth = "30rem"
        alignItems = FlexComponent.Alignment.STRETCH

        loginForm.isForgotPasswordButtonVisible = false

        val forgotPassword = Anchor(fafProperties.account().passwordResetUrl(), getTranslation("login.forgotPassword"))

        val links =
            HorizontalLayout(forgotPassword).apply {
                justifyContentMode = FlexComponent.JustifyContentMode.CENTER
                setWidthFull()
            }

        add(H2(getTranslation("ucp.login.heading")))
        add(Paragraph(getTranslation("ucp.login.description")))
        add(loginForm)
        add(links)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (ucpSessionService.isLoggedIn()) {
            event.forwardTo(UcpAccountDataView::class.java)
        }
    }
}
