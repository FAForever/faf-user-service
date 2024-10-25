package com.faforever.userservice.ui.component

import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Instance

@Dependent
class ScopeWidget(
    private val scopeItemFactory: Instance<ScopeItem>,
) : CompactVerticalLayout() {
    private val scopeLayout = CompactVerticalLayout()

    init {
        val header = Paragraph(getTranslation("consent.appRequest"))
        header.setId("scope-header")
        add(header)
        add(Hr())
        add(scopeLayout)
    }

    fun setScopes(scopes: List<String>) {
        scopeLayout.removeAll()
        scopes.forEach {
            val scopeItem = scopeItemFactory.get()
            scopeItem.setScope(it)
            add(scopeItem)
        }
    }
}

@Dependent
class ScopeItem : HorizontalLayout() {
    private val infoTooltip = InfoTooltipIcon()
    private val span = Span()

    init {
        alignItems = FlexComponent.Alignment.CENTER
        justifyContentMode = FlexComponent.JustifyContentMode.BETWEEN
        addClassNames("oauth-scope")
        add(span)
        add(infoTooltip)
    }

    fun setScope(scope: String) {
        val translation = getTranslation("oauth2.scope.$scope")
        span.text =
            if (translation.matches(Regex("!\\{.*}!"))) {
                getTranslation(
                    "oauth2.scope.textMissing",
                    scope,
                )
            } else {
                translation
            }
        val descriptionTranslation = getTranslation("oauth2.scope.$scope.description")
        if (!descriptionTranslation.isNullOrBlank() && !descriptionTranslation.matches(Regex("!\\{.*}!"))) {
            infoTooltip.setTooltip(descriptionTranslation)
        } else {
            infoTooltip.setTooltip(null)
        }
    }
}
