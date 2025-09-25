package com.faforever.userservice.ui.layout

import com.faforever.userservice.backend.i18n.I18n
import com.faforever.userservice.ui.view.ucp.AccountDataView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.tabs.Tab
import com.vaadin.flow.component.tabs.Tabs
import com.vaadin.flow.router.RouterLayout
import com.vaadin.flow.router.RouterLink
import kotlin.reflect.KClass

abstract class UcpLayout(
    private val i18n: I18n,
) : AppLayout(), RouterLayout {

    init {
        val toggle = DrawerToggle()

        val title = H1("FAF User Control Panel")
        title.style.set("font-size", "var(--lumo-font-size-l)")["margin"] = "0"

        // createLinks().forEach(::addToDrawer)
        addToDrawer(getTabs())
        addToNavbar(toggle, title)
    }

    private fun buildAnchor(href: String, i18nKey: String, icon: VaadinIcon) =
        Anchor().apply {
            setHref(href)
            add(icon.create())
            // add(i18n.getTranslation(i18nKey))
            add(i18nKey)
        }

    private fun getTabs(): Tabs {
        val tabs = Tabs()
        tabs.add(
            createTab(VaadinIcon.USER_CARD, "Account Data", AccountDataView::class),
            createTab(VaadinIcon.LINK, "Account Links", AccountDataView::class, false),
            createTab(VaadinIcon.DESKTOP, "Active Devices", AccountDataView::class, false),
            createTab(VaadinIcon.USER_HEART, "Friends & Foes", AccountDataView::class, false),
            createTab(VaadinIcon.TROPHY, "Avatars", AccountDataView::class, false),
            createTab(VaadinIcon.FILE_ZIP, "Uploaded content", AccountDataView::class, false),
            createTab(VaadinIcon.SWORD, "Moderation Reports", AccountDataView::class, false),
            createTab(VaadinIcon.KEY_O, "Permissions", AccountDataView::class, false),
            createTab(VaadinIcon.BAN, "Ban history", AccountDataView::class, false),
            createTab(VaadinIcon.EXIT_O, "Delete Account", AccountDataView::class, false),
        )
        tabs.orientation = Tabs.Orientation.VERTICAL
        return tabs
    }

    private fun createTab(
        viewIcon: VaadinIcon,
        viewName: String,
        route: KClass<out Component>,
        enabled: Boolean = true,
    ): Tab {
        val icon: Icon = viewIcon.create()
        icon.getStyle().set("box-sizing", "border-box")
            .set("margin-inline-end", "var(--lumo-space-m)")
            .set("margin-inline-start", "var(--lumo-space-xs)")
            .set("padding", "var(--lumo-space-xs)")
        val link = RouterLink()
        link.add(icon, Span(viewName))
        // Demo has no routes
        link.setRoute(route.java)
        link.tabIndex = -1
        return Tab(link).apply {
            isEnabled = enabled
        }
    }
}
