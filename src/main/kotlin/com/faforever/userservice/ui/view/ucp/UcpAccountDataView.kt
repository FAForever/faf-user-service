package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.UcpAccountDataService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route(value = "/ucp", layout = UcpLayout::class)
class UcpAccountDataView (
    private val ucpSessionService: UcpSessionService,
    private val ucpAccountDataService: UcpAccountDataService
): VerticalLayout() {

    init {
        setPadding(true)
        setSizeFull()
        add(H3(getTranslation("ucp.nav.accountData")))

        val currentUser = ucpSessionService.getCurrentUser()
        if (currentUser != null) {
            val accountData = ucpAccountDataService.getAccountData(currentUser.userId)
            if (accountData != null) {
                addAccountInfo(accountData)
            }
        }
    }

    private fun addAccountInfo(accountData: com.faforever.userservice.backend.ucp.AccountData) {
        val infoLayout = VerticalLayout().apply {
            setPadding(false)
            setWidthFull()
        }

        infoLayout.add(createInfoRow("ucp.accountData.username", accountData.username))
        infoLayout.add(createInfoRow("ucp.accountData.email", accountData.email))

        add(infoLayout)
    }

    private fun createInfoRow(labelKey: String, value: String): HorizontalLayout {
        return HorizontalLayout().apply {
            setWidthFull()
            add(Span(getTranslation(labelKey)).apply { element.style.set("font-weight", "bold") })
            add(Span(value))
        }
    }
}