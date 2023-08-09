package com.faforever.userservice.ui.component

import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

open class ErrorCard : CompactVerticalLayout() {
    private val errorLayout = HorizontalLayout()
    private val errorMessage = Span()

    private val errorTitle = H2()

    init {
        val formHeader = HorizontalLayout()

        val formHeaderLeft = FafLogo()
        formHeader.add(formHeaderLeft, errorTitle)
        formHeader.alignItems = FlexComponent.Alignment.CENTER
        formHeader.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        formHeader.setId("form-header")
        formHeader.setWidthFull()

        add(formHeader)

        errorLayout.setWidthFull()
        errorLayout.addClassNames("error", "error-info")
        errorLayout.add(errorMessage)
        errorLayout.alignItems = FlexComponent.Alignment.CENTER
        errorLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)

        add(errorLayout)
    }

    fun setMessage(message: String) {
        errorMessage.text = message
    }

    fun setTitle(title: String) {
        errorTitle.text = title
    }
}
