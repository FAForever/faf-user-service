package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.UcpAccountDataService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.formlayout.FormLayout
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
        val infoLayout = HorizontalLayout().apply {
            setWidthFull()
            alignItems = com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER
        }

        // Left side: form (username + email)
        val formLayout = FormLayout().apply {
            responsiveSteps = listOf(
                FormLayout.ResponsiveStep("0", 1)
            )

            addFormItem(Span(accountData.username), getTranslation("ucp.accountData.username"))
            addFormItem(Span(accountData.email), getTranslation("ucp.accountData.email"))
        }

        // Right side: avatar
        val avatar = if (accountData.avatarUrl != null) {
            Image(accountData.avatarUrl, "Avatar").apply {
                setWidth("100px")
                setHeight("100px")
                style.set("object-fit", "contain")
            }
        } else null

        infoLayout.add(formLayout)
        if (avatar != null) {
            infoLayout.add(avatar)
        }

        add(infoLayout)
    }
}