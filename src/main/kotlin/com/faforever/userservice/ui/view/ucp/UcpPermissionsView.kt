package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.domain.Permission
import com.faforever.userservice.backend.ucp.UcpPermissionService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/permissions", layout = UcpLayout::class)
@PermitAll
class UcpPermissionsView(
    private val ucpSessionService: UcpSessionService,
    private val ucpPermissionService: UcpPermissionService,
) : VerticalLayout(),
    BeforeEnterObserver {

    private val permissionsGrid = Grid(Permission::class.java, false).apply {
        addColumn(Permission::technicalName).setHeader(getTranslation("ucp.permissions.column.name"))
        setEmptyStateText(getTranslation("ucp.permissions.noPermissions"))
        setWidthFull()
    }

    init {
        setPadding(true)
        setSizeFull()
        add(H2(getTranslation("ucp.nav.permissions")))
        add(permissionsGrid)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = ucpSessionService.getCurrentUser()
        val permissions = ucpPermissionService.getPermissionsForUser(user.userId)
        permissionsGrid.setItems(permissions)
    }
}
