package com.faforever.userservice.ui.component

import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

class LogoHeader : HorizontalLayout() {

    private val title = H2()

    init {
        add(FafLogo(), title)
        alignItems = FlexComponent.Alignment.CENTER
        setId("form-header")
        setWidthFull()
    }

    fun setTitle(title: String) {
        this.title.text = title
    }
}
