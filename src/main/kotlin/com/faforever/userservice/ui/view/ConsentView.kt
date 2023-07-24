package com.faforever.userservice.ui.view

import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.ui.component.OAuthClientHeader
import com.faforever.userservice.ui.component.ScopeWidget
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.faforever.userservice.ui.layout.OAuthCardLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import sh.ory.hydra.model.ConsentRequest

@Route("/oauth2/consent", layout = OAuthCardLayout::class)
class ConsentView(
    private val oAuthClientHeader: OAuthClientHeader,
    private val scopeWidget: ScopeWidget,
    private val hydraService: HydraService,
) : CompactVerticalLayout(), BeforeEnterObserver {
    private val authorize = Button(getTranslation("consent.authorize")) { authorize() }
    private val deny = Button(getTranslation("consent.deny")) { deny() }

    private lateinit var challenge: String

    init {
        add(oAuthClientHeader)
        add(scopeWidget)

        authorize.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val buttonLayout = HorizontalLayout(deny, authorize)
        buttonLayout.setFlexGrow(1.0, deny, authorize)
        buttonLayout.setWidthFull()
        buttonLayout.alignItems = FlexComponent.Alignment.STRETCH
        add(buttonLayout)

        val socialIcons = SocialIcons()
        socialIcons.setWidthFull()
        add(socialIcons)
    }

    private fun setDetailsFromRequest(consentRequest: ConsentRequest) {
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
        val possibleChallenge = event?.location?.queryParameters?.parameters?.get("consent_challenge")?.get(0)
        if (possibleChallenge != null) {
            challenge = possibleChallenge
            setDetailsFromRequest(hydraService.getConsentRequest(possibleChallenge))
        } else {
            throw NoChallengeException()
        }
    }
}