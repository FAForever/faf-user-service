package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.AccountData
import com.faforever.userservice.backend.ucp.UcpAccountDataService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.Component
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

    companion object {
        private const val AVATAR_WIDTH = "120px"
        private const val AVATAR_HEIGHT = "60px"
    }

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

    private fun buildAvatarContent(url: String?, tooltip: String?): Component =
        if (url != null) {
            Image(url, tooltip ?: "").apply {
                setWidth(AVATAR_WIDTH)
                setHeight(AVATAR_HEIGHT)
                style.set("object-fit", "contain")
            }
        } else {
            Span(getTranslation("ucp.accountData.noAvatar")).apply {
                style.set("font-size", "12px")
                style.set("color", "var(--lumo-secondary-text-color)")
                style.set("text-align", "center")
            }
        }

    private fun addAccountInfo(accountData: AccountData) {
        val formLayout = FormLayout().apply {
            setAutoResponsive(true)
            setMaxColumns(1)
            addFormRow().addFormItem(Span(accountData.username), getTranslation("ucp.accountData.username"))
            addFormRow().addFormItem(Span(accountData.email), getTranslation("ucp.accountData.email"))
        }

        val avatarContent = buildAvatarContent(
            accountData.avatarUrl,
            accountData.avatarTooltip,
        )

        val avatarContainer = VerticalLayout().apply {
            isPadding = false
            isSpacing = false
            alignItems = FlexComponent.Alignment.CENTER
            width = AVATAR_WIDTH
            height = AVATAR_HEIGHT
            style.set("border-radius", "4px")
            if (accountData.avatarUrl == null) {
                style.set("border", "2px dashed var(--lumo-contrast-20pct)")
            }
            add(avatarContent)
        }

        val row = HorizontalLayout(formLayout, avatarContainer).apply {
            setWidthFull()
            alignItems = FlexComponent.Alignment.CENTER
        }

        accountRow?.let { remove(it) }
        accountRow = row
        add(row)
    }
}
