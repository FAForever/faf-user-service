package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.domain.Permission
import com.faforever.userservice.backend.ucp.UcpPermissionService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route

@Route(value = "/ucp/permissions", layout = UcpLayout::class)
class UcpPermissionsView(
    private val ucpSessionService: UcpSessionService,
    private val ucpPermissionService: UcpPermissionService,
) : VerticalLayout(),
    BeforeEnterObserver {

    private val emptyText = Paragraph(getTranslation("ucp.permissions.noPermissions")).apply {
        isVisible = false
    }

    private val permissionsGrid = Grid(Permission::class.java, false).apply {
        addColumn(Permission::technicalName).setHeader(getTranslation("ucp.permissions.column.name"))
        setWidthFull()
        isVisible = false
    }

    init {
        setPadding(true)
        setSizeFull()
        add(H2(getTranslation("ucp.nav.permissions")))
        add(emptyText)
        add(permissionsGrid)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = ucpSessionService.getCurrentUser() ?: return
        val permissions = ucpPermissionService.getPermissionsForUser(user.userId)
        if (permissions.isEmpty()) {
            emptyText.isVisible = true
        } else {
            permissionsGrid.setItems(permissions)
            permissionsGrid.isVisible = true
        }
    }
}
