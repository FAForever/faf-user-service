package com.faforever.userservice.ui.view.oauth2

import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinSession

@Route("/oauth2/device-login", layout = CardLayout::class)
class DeviceLoginView(
    private val hydraService: HydraService,
) : CompactVerticalLayout(), BeforeEnterObserver {

    init {
        add(ProgressBar().apply { isIndeterminate = true })
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val params = event.location.queryParameters.parameters
        val challenge = params["device_challenge"]?.firstOrNull()
        val userCode = params["user_code"]?.firstOrNull()

        if (challenge.isNullOrBlank() || userCode.isNullOrBlank()) {
            throw NoChallengeException()
        }

        // Carry the user code across the Hydra round-trip so the login screen can display it.
        VaadinSession.getCurrent()?.setAttribute(DEVICE_USER_CODE_SESSION_ATTR, userCode)

        val redirectTo = hydraService.acceptDeviceRequest(challenge, userCode)
        event.ui.page.setLocation(redirectTo.uri)
    }

    companion object {
        const val DEVICE_USER_CODE_SESSION_ATTR = "oauth2.deviceUserCode"
    }
}
