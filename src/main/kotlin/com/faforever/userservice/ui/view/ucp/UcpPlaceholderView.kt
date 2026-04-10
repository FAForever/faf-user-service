package com.faforever.userservice.ui.view.ucp

import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.VerticalLayout

open class UcpPlaceholderView(
    private val titleKey: String,
) : VerticalLayout() {
    init {
        setPadding(true)
        setSizeFull()
        add(H2(getTranslation(titleKey)))
        add(Paragraph(getTranslation("ucp.placeholder")))
    }
}
