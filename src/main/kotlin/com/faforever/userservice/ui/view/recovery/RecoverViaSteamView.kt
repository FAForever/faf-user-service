package com.faforever.userservice.ui.view.recovery

import com.faforever.userservice.backend.account.RecoveryService
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.HtmlComponent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route("/recover-account/steam", layout = CardLayout::class)
class RecoverViaSteamView(
    private val recoveryService: RecoveryService,
) : CompactVerticalLayout() {
    companion object {
        const val STEAM_SIGNIN_LOGO_URL =
            "https://community.cloudflare.steamstatic.com/public/images/signinthroughsteam/sits_01.png"
    }

    private val submit =
        Button(
            Image(STEAM_SIGNIN_LOGO_URL, "Steam Sign In Logo"),
            { redirectToSteam() },
        ).apply {
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            setWidthFull()
            style.setPaddingTop("30px")
            style.setPaddingBottom("26px")
        }

    init {
        maxWidth = "30rem"

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("recovery.steam.title"))
        val formHeader =
            HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
                justifyContentMode = FlexComponent.JustifyContentMode.CENTER
                alignItems = FlexComponent.Alignment.CENTER
                setId("form-header")
                setWidthFull()
            }

        add(formHeader)
        add(
            Paragraph(
                HtmlComponent("small").apply {
                    element.setProperty("innerHTML", getTranslation("recovery.steam.disclaimer"))
                },
            ),
        )
        add(submit)

        val footer =
            VerticalLayout(SocialIcons()).apply {
                alignItems = FlexComponent.Alignment.CENTER
            }

        add(footer)
    }

    private fun redirectToSteam() {
        val steamUrl = recoveryService.buildSteamLoginUrl()
        UI.getCurrent().page.setLocation(steamUrl)
    }
}
