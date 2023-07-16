package com.faforever.userservice.ui.component

import com.faforever.userservice.backend.i18n.I18n
import com.vaadin.flow.component.html.Hr
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.spi.CDI

@Dependent
class ScopeWidget(i18n: I18n) : CompactVerticalLayout() {
    private val scopeLayout = CompactVerticalLayout()

    init {
        val header = Paragraph(i18n.getTranslation("consent.appRequest"))
        header.setId("scope-header")
        add(header)
        add(Hr())
        add(scopeLayout)
    }

    fun setScopes(scopes: List<String>) {
        scopeLayout.removeAll()
        scopes.forEach {
            val scopeItem = CDI.current().select(ScopeItem::class.java).get()
            scopeItem.setScope(it)
            add(scopeItem)
        }
    }

}

@Dependent
@Unremovable
class ScopeItem(private val i18n: I18n) : HorizontalLayout() {
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
        span.text = i18n.getTranslation("oauth2.scope.$scope") ?: i18n.getTranslation("oauth2.scope.textMissing", scope)
        infoTooltip.setTooltip(i18n.getTranslation("oauth2.scope.$scope.description"))
    }

}