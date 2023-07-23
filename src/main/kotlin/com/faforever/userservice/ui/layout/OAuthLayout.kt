package com.faforever.userservice.ui.layout

import com.faforever.userservice.ui.component.FafLogo
import com.vaadin.flow.component.Unit
import com.vaadin.flow.component.html.*
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.ParentLayout
import com.vaadin.flow.router.RouterLayout

class OAuthLayout : VerticalLayout(), RouterLayout {

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

        add(OAuthHeader())
    }
}

class OAuthHeader : Header() {
    init {
        setWidthFull()
        setHeight(50f, Unit.PIXELS)

        val leftHeader = Div()
        leftHeader.setId("leftheader")

        val imageLink =
            Anchor("https://www.faforever.com", FafLogo())
        leftHeader.add(imageLink)

        val environment = H1("FAForever Login")
        leftHeader.add(environment)

        add(leftHeader)
    }
}

@ParentLayout(OAuthLayout::class)
class OAuthCardLayout : VerticalLayout(), RouterLayout {

    init {
        addClassName("main-card")
    }
}