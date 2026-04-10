package com.faforever.userservice.ui.layout

import com.faforever.userservice.ui.view.ucp.UcpAccountDataView
import com.faforever.userservice.ui.view.ucp.UcpAccountLinkingView
import com.faforever.userservice.ui.view.ucp.UcpAvatarsView
import com.faforever.userservice.ui.view.ucp.UcpBanHistoryView
import com.faforever.userservice.ui.view.ucp.UcpChangeEmailView
import com.faforever.userservice.ui.view.ucp.UcpChangePasswordView
import com.faforever.userservice.ui.view.ucp.UcpChangeUsernameView
import com.faforever.userservice.ui.view.ucp.UcpDeleteAccountView
import com.faforever.userservice.ui.view.ucp.UcpFriendsFoesView
import com.faforever.userservice.ui.view.ucp.UcpPermissionsView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.router.RouterLayout
import jakarta.enterprise.context.Dependent

@Dependent
class UcpLayout : AppLayout(), RouterLayout {

    init {
        val toggle = DrawerToggle()
        val title = H1(getTranslation("ucp.title"))
        title.style.set("margin", "0")
        title.style.set("font-size", "var(--lumo-font-size-l)")
        addToNavbar(true, toggle, title)

        val nav = SideNav()
        nav.addItem(navItem("ucp.nav.accountData", VaadinIcon.USER_CARD, UcpAccountDataView::class.java))
        nav.addItem(navItem("ucp.nav.changeUsername", VaadinIcon.EDIT, UcpChangeUsernameView::class.java))
        nav.addItem(navItem("ucp.nav.changeEmail", VaadinIcon.ENVELOPE, UcpChangeEmailView::class.java))
        nav.addItem(navItem("ucp.nav.changePassword", VaadinIcon.KEY, UcpChangePasswordView::class.java))
        nav.addItem(navItem("ucp.nav.accountLinking", VaadinIcon.LINK, UcpAccountLinkingView::class.java))
        nav.addItem(navItem("ucp.nav.friendsFoes", VaadinIcon.HEART, UcpFriendsFoesView::class.java))
        nav.addItem(navItem("ucp.nav.avatars", VaadinIcon.PICTURE, UcpAvatarsView::class.java))
        nav.addItem(navItem("ucp.nav.permissions", VaadinIcon.KEY_O, UcpPermissionsView::class.java))
        nav.addItem(navItem("ucp.nav.banHistory", VaadinIcon.BAN, UcpBanHistoryView::class.java))
        nav.addItem(navItem("ucp.nav.deleteAccount", VaadinIcon.EXIT_O, UcpDeleteAccountView::class.java))
        addToDrawer(nav)
    }

    private fun navItem(i18nKey: String, icon: VaadinIcon, target: Class<out Component>): SideNavItem {
        val item = SideNavItem(getTranslation(i18nKey), target)
        item.prefixComponent = icon.create()
        return item
    }
}
