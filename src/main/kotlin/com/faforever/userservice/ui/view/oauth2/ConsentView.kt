package com.faforever.userservice.ui.view.oauth2

import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.ui.component.OAuthClientHeader
import com.faforever.userservice.ui.component.ScopeWidget
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinSession
import sh.ory.hydra.model.OAuth2ConsentRequest

@Route("/oauth2/consent", layout = CardLayout::class)
class ConsentView(
    private val oAuthClientHeader: OAuthClientHeader,
    private val scopeWidget: ScopeWidget,
    private val hydraService: HydraService,
) : CompactVerticalLayout(),
    BeforeEnterObserver {
    private val authorize =
        Button(getTranslation("consent.authorize")) { authorize() }.apply {
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            addClickShortcut(Key.ENTER)
        }
    private val deny = Button(getTranslation("consent.deny")) { deny() }

    private lateinit var challenge: String

    init {
        add(oAuthClientHeader)
        add(scopeWidget)

        val buttonLayout =
            HorizontalLayout(deny, authorize).apply {
                alignItems = FlexComponent.Alignment.STRETCH
                setFlexGrow(1.0, deny, authorize)
                setWidthFull()
            }

        add(buttonLayout)

        val socialIcons =
            SocialIcons().apply {
                setWidthFull()
            }
        add(socialIcons)
    }

    private fun setDetailsFromRequest(consentRequest: OAuth2ConsentRequest) {
        consentRequest.client?.let { oAuthClientHeader.setClient(it) }

        if (consentRequest.requestedScope.isNullOrEmpty()) {
            scopeWidget.setScopes(consentRequest.client?.scope?.split(" ") ?: emptyList())
        } else {
            scopeWidget.setScopes(consentRequest.requestedScope)
        }
    }

    private fun authorize() {
        val redirectTo = hydraService.acceptConsentRequest(challenge)
        ui.ifPresent { it.page.setLocation(redirectTo.uri) }
    }

    private fun deny() {
        val redirectTo = hydraService.denyConsentRequest(challenge)
        ui.ifPresent { it.page.setLocation(redirectTo.uri) }
    }

    override fun beforeEnter(event: BeforeEnterEvent?) {
        val possibleChallenge =
            event
                ?.location
                ?.queryParameters
                ?.parameters
                ?.get("consent_challenge")
                ?.get(0)
        if (possibleChallenge != null) {
            challenge = possibleChallenge
            setDetailsFromRequest(hydraService.getConsentRequest(challenge))
            // Defensive cleanup: if Hydra skipped the login screen (existing SSO session),
            // LoginView never consumed the device user code. Clear it so it can't leak into
            // a later, unrelated login in this browser session.
            VaadinSession.getCurrent()?.setAttribute(DeviceLoginView.DEVICE_USER_CODE_SESSION_ATTR, null)
        } else {
            throw NoChallengeException()
        }
    }
}
