package com.faforever.userservice.ui.view.oauth2

import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.ui.component.LogoHeader
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route

@Route("/oauth2/device-login", layout = CardLayout::class)
class DeviceLoginView(
    private val hydraService: HydraService,
) : CompactVerticalLayout(), BeforeEnterObserver {

    private val header = LogoHeader().apply {
        setTitle(getTranslation("device-login.title"))
    }

    private val userCodeSpan = Span()

    private val authorizeButton = Button(getTranslation("device-login.authorizeAction")) { authorize() }.apply {
        setWidthFull()
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
    }

    private lateinit var challenge: String
    private lateinit var userCode: String

    init {
        add(header)
        add(userCodeSpan)
        add(authorizeButton)
    }

    private fun authorize() {
        val redirectTo = hydraService.acceptDeviceRequest(challenge, userCode)
        ui.ifPresent { it.page.setLocation(redirectTo.uri) }
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val params = event?.location?.queryParameters?.parameters
        val possibleChallenge = params?.get("device_challenge")?.firstOrNull()
        val possibleUserCode = params?.get("user_code")?.firstOrNull()

        if (possibleChallenge.isNullOrBlank() || possibleUserCode.isNullOrBlank()) {
            throw NoChallengeException()
        }

        challenge = possibleChallenge
        userCode = possibleUserCode
        userCodeSpan.text = getTranslation("device-login.confirm", userCode)
    }
}
