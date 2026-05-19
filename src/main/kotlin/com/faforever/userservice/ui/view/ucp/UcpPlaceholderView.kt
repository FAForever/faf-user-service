package com.faforever.userservice.ui.view.ucp

import com.vaadin.flow.component.AttachEvent
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.i18n.LocaleChangeEvent
import com.vaadin.flow.i18n.LocaleChangeObserver

open class UcpPlaceholderView(
    private val titleKey: String,
) : VerticalLayout(),
    LocaleChangeObserver {
    private val title = H2()
    private val body = Paragraph()

    init {
        setPadding(true)
        setSizeFull()
        add(title)
        add(body)
    }

    override fun onAttach(attachEvent: AttachEvent) {
        super.onAttach(attachEvent)
        updateTranslations()
    }

    override fun localeChange(event: LocaleChangeEvent) {
        updateTranslations()
    }

    private fun updateTranslations() {
        title.text = getTranslation(titleKey)
        body.text = getTranslation("ucp.placeholder")
    }
}
