package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.domain.Group
import com.faforever.userservice.backend.ucp.UcpGroupsService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/groups", layout = UcpLayout::class)
@PermitAll
class UcpGroupsView(
    private val ucpSessionService: UcpSessionService,
    private val ucpGroupsService: UcpGroupsService,
) : VerticalLayout(),
    BeforeEnterObserver {

    private val groupsGrid = Grid(Group::class.java, false).apply {
        addColumn(Group::technicalName).setHeader(getTranslation("ucp.groups.column.name"))
        setEmptyStateText(getTranslation("ucp.groups.noGroups"))
        setWidthFull()
    }

    init {
        setPadding(true)
        setSizeFull()
        add(H2(getTranslation("ucp.nav.groups")))
        add(groupsGrid)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = ucpSessionService.getCurrentUser()
        val groups = ucpGroupsService.getGroupsForUser(user.userId)
        groupsGrid.setItems(groups)
    }
}
