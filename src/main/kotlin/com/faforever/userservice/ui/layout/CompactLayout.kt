package com.faforever.userservice.ui.layout

import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout

open class CompactVerticalLayout : VerticalLayout() {
    init {
        isPadding = false
        isSpacing = false
    }
}

open class CompactHorizontalLayout : HorizontalLayout() {
    init {
        isPadding = false
        isSpacing = false
    }
}