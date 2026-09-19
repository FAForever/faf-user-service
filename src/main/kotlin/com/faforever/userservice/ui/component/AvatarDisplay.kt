package com.faforever.userservice.ui.component

import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout

class AvatarDisplay(
    url: String?,
    tooltip: String?,
    width: String,
    height: String,
) : VerticalLayout() {
    init {
        isPadding = false
        isSpacing = false
        alignItems = FlexComponent.Alignment.CENTER

        if (url != null) {
            add(
                Image(url, tooltip ?: "").apply {
                    setWidth(width)
                    setHeight(height)
                    style.set("object-fit", "contain")
                },
            )
        } else {
            add(
                Span(getTranslation("ucp.accountData.noAvatar")).apply {
                    style.set("font-size", "12px")
                    style.set("color", "var(--lumo-secondary-text-color)")
                    style.set("text-align", "center")
                },
            )
        }
    }
}
