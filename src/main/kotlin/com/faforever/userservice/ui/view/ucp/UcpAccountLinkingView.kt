package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.UcpGogLinkService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.backend.ucp.UcpSteamLinkService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HtmlComponent
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
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
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/linking", layout = UcpLayout::class)
@PermitAll
class UcpAccountLinkingView(
    private val ucpSessionService: UcpSessionService,
    private val ucpSteamLinkService: UcpSteamLinkService,
    private val ucpGogLinkService: UcpGogLinkService,
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
        services.add(buildGogCard(userId))
    }

    private fun showResult(code: String) {
        if (code !in RESULT_CODES) return
        Notification.show(
            getTranslation("ucp.accountLinking.result.$code"),
            NOTIFICATION_DURATION_MS,
            Notification.Position.MIDDLE,
        )
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

    private fun buildGogCard(userId: Int): Component {
        val content = when (val status = ucpGogLinkService.getStatus(userId)) {
            is UcpGogLinkService.GogLinkStatus.Linked ->
                connectedContent(getTranslation("ucp.accountLinking.gog.id", status.gogUsername))
            UcpGogLinkService.GogLinkStatus.NotLinked -> gogConnectContent(userId)
        }
        return serviceCard(getTranslation("ucp.accountLinking.gog.title"), content)
    }

    private fun gogConnectContent(userId: Int): Component {
        val usernameField = TextField(getTranslation("ucp.accountLinking.gog.username")).apply {
            setWidthFull()
        }
        val verificationToken = ucpGogLinkService.buildGogToken(userId)
        val tokenHint = Paragraph().apply {
            style.set("margin", "0")
            add(
                Span(getTranslation("ucp.accountLinking.gog.tokenLabel") + " ").apply {
                    style.set("font-weight", "bold")
                },
            )
            add(Span(verificationToken))
        }
        val instructions = buildGogInstructions()
        val verifyButton = Button(getTranslation("ucp.accountLinking.gog.verify")) {
            val result = ucpGogLinkService.linkToGog(userId, usernameField.value)
            when (result) {
                UcpGogLinkService.LinkResult.Success -> {
                    Notification.show(
                        getTranslation("ucp.accountLinking.result.gogSuccess"),
                        NOTIFICATION_DURATION_MS,
                        Notification.Position.MIDDLE,
                    ).addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                    services.removeAll()
                    services.add(buildSteamCard(userId))
                    services.add(buildGogCard(userId))
                }
                UcpGogLinkService.LinkResult.InvalidUsername -> Notification.show(
                    getTranslation("ucp.accountLinking.result.gogInvalidUsername"),
                    NOTIFICATION_DURATION_MS,
                    Notification.Position.MIDDLE,
                ).addThemeVariants(NotificationVariant.LUMO_ERROR)
                UcpGogLinkService.LinkResult.ProfileTokenNotSet -> Notification.show(
                    getTranslation("ucp.accountLinking.result.gogProfileTokenNotSet"),
                    NOTIFICATION_DURATION_MS,
                    Notification.Position.MIDDLE,
                ).addThemeVariants(NotificationVariant.LUMO_ERROR)
                UcpGogLinkService.LinkResult.NoGameOwnership -> Notification.show(
                    getTranslation("ucp.accountLinking.result.gogNoGameOwnership"),
                    NOTIFICATION_DURATION_MS,
                    Notification.Position.MIDDLE,
                ).addThemeVariants(NotificationVariant.LUMO_ERROR)
                UcpGogLinkService.LinkResult.AlreadyLinkedToOther -> Notification.show(
                    getTranslation("ucp.accountLinking.result.gogAlreadyLinkedToOther"),
                    NOTIFICATION_DURATION_MS,
                    Notification.Position.MIDDLE,
                ).addThemeVariants(NotificationVariant.LUMO_ERROR)
                UcpGogLinkService.LinkResult.Failed -> Notification.show(
                    getTranslation("ucp.accountLinking.result.gogFailed"),
                    NOTIFICATION_DURATION_MS,
                    Notification.Position.MIDDLE,
                ).addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
        return VerticalLayout(usernameField, tokenHint, instructions, verifyButton).apply {
            isPadding = false
            isSpacing = true
            style.set("overflow", "visible")
        }
    }

    private data class InstructionSegment(val text: String, val bold: Boolean = false)

    private fun buildGogInstructions(): Component {
        var activePopup: VerticalLayout? = null

        fun openPopup(popup: VerticalLayout) {
            if (activePopup !== popup) {
                activePopup?.isVisible = false
            }
            activePopup = popup
        }

        val line1 = buildInstructionLine(
            listOf(
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line1.number") + " ", true),
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line1.text") + " "),
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line1.highlight"), true),
            ),
            getTranslation("ucp.accountLinking.gog.instructions.help.public.title"),
            getTranslation("ucp.accountLinking.gog.instructions.help.public.text"),
            getTranslation("ucp.accountLinking.gog.instructions.help.public.aria"),
            ::openPopup,
        )
        val line2 = buildInstructionLine(
            listOf(
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line2.number") + " ", true),
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line2.text") + " "),
                InstructionSegment(
                    getTranslation("ucp.accountLinking.gog.instructions.line2.highlight") + " ",
                    true,
                ),
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line2.suffix")),
            ),
            getTranslation("ucp.accountLinking.gog.instructions.help.aboutYou.title"),
            getTranslation("ucp.accountLinking.gog.instructions.help.aboutYou.text"),
            getTranslation("ucp.accountLinking.gog.instructions.help.aboutYou.aria"),
            ::openPopup,
        )
        val line3 = buildInstructionText(
            listOf(
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line3.number") + " ", true),
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line3.text") + " "),
                InstructionSegment(getTranslation("ucp.accountLinking.gog.verify"), true),
            ),
        )
        val line4 = buildInstructionText(
            listOf(
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line4.number") + " ", true),
                InstructionSegment(getTranslation("ucp.accountLinking.gog.instructions.line4.text")),
            ),
        )
        return VerticalLayout(line1, line2, line3, line4).apply {
            isPadding = false
            isSpacing = true
            setWidthFull()
            style.set("overflow", "visible")
        }
    }

    private fun buildInstructionText(segments: List<InstructionSegment>): Component {
        return Paragraph().apply {
            style.set("margin", "0")
            style.set("white-space", "pre-wrap")
            segments.forEach { segment ->
                add(
                    Span(segment.text).apply {
                        if (segment.bold) {
                            style.set("font-weight", "bold")
                        }
                        style.set("display", "inline")
                    },
                )
            }
        }
    }

    private fun buildInstructionLine(
        segments: List<InstructionSegment>,
        tooltipTitle: String,
        tooltipText: String,
        helpButtonAriaLabel: String,
        onOpenPopup: (VerticalLayout) -> Unit,
    ): Component {
        val popup = buildInstructionPopup(tooltipTitle, tooltipText)
        val helpButton = Button(VaadinIcon.QUESTION_CIRCLE_O.create()) {
            val show = !popup.isVisible
            if (show) {
                onOpenPopup(popup)
            }
            popup.isVisible = show
        }.apply {
            element.setAttribute("aria-label", helpButtonAriaLabel)
            style.set("min-width", "2rem")
            style.set("height", "2rem")
            style.set("width", "2rem")
            style.set("border-radius", "50%")
            style.set("padding", "0")
            style.set("line-height", "1")
            style.set("font-size", "1rem")
            style.set("box-sizing", "border-box")
        }
        val popupWrapper = Div(helpButton, popup).apply {
            style.set("position", "relative")
            style.set("display", "inline-flex")
            style.set("align-items", "flex-start")
        }
        val instructionText = buildInstructionText(segments)
        instructionText.style.set("min-width", "0")
        return HorizontalLayout(instructionText, popupWrapper).apply {
            isPadding = false
            isSpacing = true
            defaultVerticalComponentAlignment = FlexComponent.Alignment.CENTER
            setWidthFull()
            expand(instructionText)
            style.set("overflow", "visible")
        }
    }

    private fun buildInstructionPopup(title: String, text: String): VerticalLayout {
        return VerticalLayout().apply {
            isPadding = true
            isSpacing = false
            style.set("border", "1px solid var(--lumo-contrast-20pct)")
            style.set("border-radius", "var(--lumo-border-radius-s)")
            style.set("background-color", "var(--lumo-base-color)")
            style.set("box-shadow", "var(--lumo-box-shadow-s)")
            style.set("width", "18rem")
            style.set("max-width", "calc(100vw - 2rem)")
            style.set("box-sizing", "border-box")
            style.set("font-size", "var(--lumo-font-size-s)")
            style.set("margin-top", "0")
            style.set("left", "calc(100% + 0.5rem)")
            style.set("top", "0")
            style.set("position", "absolute")
            style.set("z-index", "10")
            isVisible = false
            add(
                Span(title).apply {
                    style.set("font-weight", "600")
                    style.set("font-size", "var(--lumo-font-size-s)")
                },
            )
            add(
                Paragraph(text).apply {
                    style.set("margin", "0")
                    style.set("font-size", "var(--lumo-font-size-s)")
                    style.set("white-space", "pre-line")
                },
            )
        }
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
