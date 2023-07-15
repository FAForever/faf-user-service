package com.faforever.userservice.ui.component

import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

class ScopeWidget : CompactVerticalLayout() {
    private val scopeLayout = CompactVerticalLayout()

    init {
        val header = Paragraph("This app would like to:")
        header.setId("scope-header")
        add(header)
        add(Hr())
        add(scopeLayout)
    }

    fun setScopes(scopes: List<String>) {
        scopeLayout.removeAll()
        scopes.forEach { scopeLayout.add(ScopeItem(it)) }
    }

}

class ScopeItem(val scope: String) : HorizontalLayout() {
    init {
        alignItems = FlexComponent.Alignment.CENTER
        justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        addClassNames("oauth-scope")
        add(scope)
        add(InfoTooltipIcon(scope))
    }

}