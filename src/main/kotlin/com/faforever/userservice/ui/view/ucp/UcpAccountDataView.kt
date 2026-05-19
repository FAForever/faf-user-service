package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.AccountData
import com.faforever.userservice.backend.ucp.UcpAccountDataService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.formlayout.FormLayout
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp", layout = UcpLayout::class)
@PermitAll
class UcpAccountDataView(
    private val ucpSessionService: UcpSessionService,
    private val ucpAccountDataService: UcpAccountDataService,
) : VerticalLayout(),
    BeforeEnterObserver {

    private var accountRow: HorizontalLayout? = null

    init {
        setPadding(true)
        setSizeFull()
        add(H2(getTranslation("ucp.nav.accountData")))
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = ucpSessionService.getCurrentUser()
        val accountData = ucpAccountDataService.getAccountData(user.userId)
        addAccountInfo(accountData)
    }

    private fun addAccountInfo(accountData: AccountData) {
        val formLayout = FormLayout().apply {
            setAutoResponsive(true)
            setMaxColumns(1)
            addFormRow().addFormItem(Span(accountData.username), getTranslation("ucp.accountData.username"))
            addFormRow().addFormItem(Span(accountData.email), getTranslation("ucp.accountData.email"))
        }

        val row = HorizontalLayout(formLayout).apply {
            setWidthFull()
            alignItems = FlexComponent.Alignment.CENTER
        }

        if (accountData.avatarUrl != null) {
            val avatarAltText = accountData.avatarTooltip?.takeIf { it.isNotBlank() } ?: accountData.username
            val avatar = Image(accountData.avatarUrl, avatarAltText).apply {
                setWidth("100px")
                setHeight("100px")
                style.set("object-fit", "contain")
            }
            row.add(avatar)
        }

        accountRow?.let { remove(it) }
        accountRow = row
        add(row)
    }
}
