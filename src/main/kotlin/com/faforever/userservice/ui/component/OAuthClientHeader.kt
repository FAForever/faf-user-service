package com.faforever.userservice.ui.component

import com.faforever.userservice.backend.i18n.I18n
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import jakarta.enterprise.context.Dependent
import sh.ory.hydra.model.OAuth2Client

@Dependent
class OAuthClientHeader(private val i18n: I18n) : CompactVerticalLayout() {
    private val clientName = H2()
    private val clientLogo = Image()
    private val clientUrl = Anchor()
    private val clientTos = Anchor()
    private val clientPolicy = Anchor()


    init {
        clientTos.text = i18n.getTranslation("consent.termsOfService")
        clientPolicy.text = i18n.getTranslation("consent.privacyStatement")

        clientLogo.width = "40px"
        clientLogo.height = "40px"

        alignItems = FlexComponent.Alignment.CENTER

        add(clientName)
        add(Hr())

        val detailsLayout = CompactVerticalLayout()
        detailsLayout.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        detailsLayout.add(clientUrl)
        detailsLayout.add(HorizontalLayout(clientTos, clientPolicy))

        add(HorizontalLayout(clientLogo, detailsLayout))
    }

    fun setClient(client: OAuth2Client) {
        clientName.text = client.clientName
        clientLogo.src = client.logoUri
        clientLogo.setAlt(i18n.getTranslation("consent.clientLogo"))

        clientUrl.href = client.clientUri
        clientUrl.text = client.clientUri
        clientUrl.isVisible = !clientUrl.href.isNullOrBlank()
        clientTos.href = client.tosUri
        clientTos.isVisible = !clientTos.href.isNullOrBlank()
        clientPolicy.href = client.policyUri
        clientPolicy.isVisible = !clientPolicy.href.isNullOrBlank()
    }

}