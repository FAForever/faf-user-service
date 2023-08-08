package com.faforever.userservice.ui.layout

import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.ui.component.FafLogo
import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.ParentLayout
import com.vaadin.flow.router.RouterLayout
import jakarta.enterprise.context.Dependent

@Dependent
class OAuthLayout(oAuthHeader: OAuthHeader) : VerticalLayout(), RouterLayout {

    companion object {
        val BACKGROUND_IMAGES = arrayOf(
            "https://content.faforever.com/images/background/background-aeon-uef.jpg",
            "https://content.faforever.com/images/background/background-aeon.jpg",
            "https://content.faforever.com/images/background/background-cybran.jpg",
            "https://content.faforever.com/images/background/background-seraphim.jpg",
            "https://content.faforever.com/images/background/background-uef.jpg",
        )
    }

    init {
        setHeightFull()
        setWidthFull()
        addClassName("background")
        style.set("background-image", "url(${BACKGROUND_IMAGES.random()})")

        add(oAuthHeader)
    }
}

@Dependent
class OAuthHeader(fafProperties: FafProperties) : Header() {
    init {
        setWidthFull()
        setHeight(50f, Unit.PIXELS)

        val leftHeader = Div()
        leftHeader.setId("leftheader")

        val imageLink = Anchor("https://www.faforever.com", FafLogo())
        leftHeader.add(imageLink)

        val environment = fafProperties.environment()
            .map { it.ifBlank { null } }
            .map { it?.uppercase() }
            .map { "[$it] FAForever" }
            .orElse("FAForever")

        leftHeader.add(H1(environment))

        add(leftHeader)
    }
}

@ParentLayout(OAuthLayout::class)
class OAuthCardLayout : VerticalLayout(), RouterLayout {

    init {
        addClassName("main-card")
    }
}
