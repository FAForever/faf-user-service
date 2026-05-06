package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.domain.Group
import com.faforever.userservice.backend.ucp.UcpGroupsService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route

@Route(value = "/ucp/groups", layout = UcpLayout::class)
class UcpGroupsView(
    private val ucpSessionService: UcpSessionService,
    private val ucpGroupsService: UcpGroupsService,
) : VerticalLayout(),
    BeforeEnterObserver {

    private val emptyText = Paragraph(getTranslation("ucp.groups.noGroups")).apply {
        isVisible = false
    }

    private val groupsGrid = Grid(Group::class.java, false).apply {
        addColumn(Group::technicalName).setHeader(getTranslation("ucp.groups.column.name"))
        setWidthFull()
        isVisible = false
    }

    init {
        setPadding(true)
        setSizeFull()
        add(H2(getTranslation("ucp.nav.groups")))
        add(emptyText)
        add(groupsGrid)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = ucpSessionService.getCurrentUser() ?: return
        val groups = ucpGroupsService.getGroupsForUser(user.userId)

        if (groups.isEmpty()) {
            emptyText.isVisible = true
            groupsGrid.isVisible = false
        } else {
            emptyText.isVisible = false
            groupsGrid.setItems(groups)
            groupsGrid.isVisible = true
        }
    }
}
