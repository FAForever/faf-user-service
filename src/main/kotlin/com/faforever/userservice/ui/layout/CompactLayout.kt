package com.faforever.userservice.ui.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout

open class CompactVerticalLayout(vararg children: Component) : VerticalLayout(*children) {
    init {
        isPadding = false
        isSpacing = false
    }
}

open class CompactHorizontalLayout(vararg children: Component) : HorizontalLayout(*children) {
    init {
        isPadding = false
        isSpacing = false
    }
}
