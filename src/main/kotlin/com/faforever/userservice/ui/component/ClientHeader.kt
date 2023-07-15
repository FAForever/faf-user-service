package com.faforever.userservice.ui.component

import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import sh.ory.hydra.model.OAuth2Client

class ClientHeader : CompactVerticalLayout() {
    private val clientName = H2()
    private val clientLogo = Image()
    private val clientUrl = Anchor()
    private val clientTos = Anchor()
    private val clientPolicy = Anchor()


    init {
        clientTos.text = "Terms of Service"
        clientPolicy.text = "Privacy Policy"

        clientLogo.width = "40px"
        clientLogo.height = "40px"

        alignItems = FlexComponent.Alignment.CENTER

        add(clientName)
        add(Hr())

        val detailsLayout = VerticalLayout()
        detailsLayout.add(clientUrl)
        detailsLayout.add(HorizontalLayout(clientTos, clientPolicy))

        add(HorizontalLayout(clientLogo, detailsLayout))
    }

    fun setClient(client: OAuth2Client) {
        clientName.text = client.clientName
        clientLogo.src = client.logoUri
        clientLogo.setAlt(client.clientName + " Logo")

        clientUrl.href = client.clientUri
        clientUrl.isVisible = !clientUrl.href.isNullOrBlank()
        clientTos.href = client.tosUri
        clientTos.isVisible = !clientTos.href.isNullOrBlank()
        clientPolicy.href = client.policyUri
        clientPolicy.isVisible = !clientPolicy.href.isNullOrBlank()
    }

}