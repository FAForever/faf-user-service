package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.backend.ucp.UcpSteamLinkService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HtmlComponent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/linking", layout = UcpLayout::class)
@PermitAll
class UcpAccountLinkingView(
    private val ucpSessionService: UcpSessionService,
    private val ucpSteamLinkService: UcpSteamLinkService,
) : VerticalLayout(), BeforeEnterObserver {

    companion object {
        const val RESULT_QUERY_PARAM = "result"
        private const val NOTIFICATION_DURATION_MS = 5000
        private const val STEAM_SIGNIN_LOGO_URL =
            "https://community.steamstatic.com/public/images/signinthroughsteam/sits_01.png"
        private val RESULT_CODES = setOf("noGameOwnership", "alreadyLinkedToOther", "failed")
    }

    private val services = VerticalLayout().apply {
        isPadding = false
        isSpacing = true
        setWidthFull()
    }

    init {
        setPadding(true)
        add(
            H2(getTranslation("ucp.nav.accountLinking")),
            Paragraph(getTranslation("ucp.accountLinking.description")),
            services,
        )
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val userId = ucpSessionService.getCurrentUser().userId
        event.location.queryParameters.parameters[RESULT_QUERY_PARAM]?.firstOrNull()?.let(::showResult)
        services.removeAll()
        services.add(buildSteamCard(userId))
    }

    private fun showResult(code: String) {
        if (code !in RESULT_CODES) return
        Notification.show(getTranslation("ucp.accountLinking.result.$code"), NOTIFICATION_DURATION_MS, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_ERROR)
    }

    private fun buildSteamCard(userId: Int): Component {
        val content = when (val status = ucpSteamLinkService.getStatus(userId)) {
            is UcpSteamLinkService.SteamLinkStatus.Linked ->
                connectedContent(getTranslation("ucp.accountLinking.steam.id", status.steamId))
            UcpSteamLinkService.SteamLinkStatus.NotLinked ->
                steamConnectContent(userId)
        }
        return serviceCard(getTranslation("ucp.accountLinking.steam.title"), content)
    }

    private fun connectedContent(detail: String): Component {
        val check = VaadinIcon.CHECK_CIRCLE.create().apply {
            style.set("color", "var(--lumo-success-color)")
        }
        val badge = HorizontalLayout(check, Span(getTranslation("ucp.accountLinking.connected"))).apply {
            isPadding = false
            alignItems = FlexComponent.Alignment.CENTER
        }
        val idLine = Span(detail).apply {
            style.set("color", "var(--lumo-secondary-text-color)")
            style.set("font-size", "var(--lumo-font-size-s)")
        }
        return VerticalLayout(badge, idLine).apply {
            isPadding = false
            isSpacing = false
        }
    }

    private fun steamConnectContent(userId: Int): Component {
        val button = Button(
            Image(STEAM_SIGNIN_LOGO_URL, getTranslation("ucp.accountLinking.steam.button")),
            { ui.ifPresent { it.page.setLocation(ucpSteamLinkService.buildSteamLinkUrl(userId)) } },
        )
        val hint = Paragraph(
            HtmlComponent("small").apply {
                element.setProperty("innerHTML", getTranslation("ucp.accountLinking.steam.disclaimer"))
            },
        )
        return VerticalLayout(button, hint).apply {
            isPadding = false
            isSpacing = false
        }
    }

    private fun serviceCard(title: String, content: Component): Component =
        VerticalLayout(H3(title), content).apply {
            isPadding = true
            isSpacing = true
            maxWidth = "32rem"
            style.set("border", "1px solid var(--lumo-contrast-20pct)")
            style.set("border-radius", "var(--lumo-border-radius-l)")
        }
}
