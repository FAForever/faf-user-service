package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.UcpSteamLinkService
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed

// Where steam drops the user after the openid.
// It is cross site so the session cannot be trusted - hence why it is anonymous.
// The user comes from the token in the url.
// They are then linked and redirected to the linking page.
@Route("/ucp/linking/confirm")
@AnonymousAllowed
class UcpLinkSteamCallbackView(
    private val ucpSteamLinkService: UcpSteamLinkService,
) : Div(), BeforeEnterObserver {

    override fun beforeEnter(event: BeforeEnterEvent) {
        // success
        val error = when (ucpSteamLinkService.linkToSteam(event.location.queryParameters.parameters)) {
            UcpSteamLinkService.LinkResult.Success -> null
            UcpSteamLinkService.LinkResult.NoGameOwnership -> "noGameOwnership"
            UcpSteamLinkService.LinkResult.AlreadyLinkedToOther -> "alreadyLinkedToOther"
            UcpSteamLinkService.LinkResult.Failed -> "failed"
        }
        // browser redirect so the session cookie is sent
        val target = error?.let { "/ucp/linking?${UcpAccountLinkingView.RESULT_QUERY_PARAM}=$it" } ?: "/ucp/linking"
        event.ui.page.setLocation(target)
    }
}
