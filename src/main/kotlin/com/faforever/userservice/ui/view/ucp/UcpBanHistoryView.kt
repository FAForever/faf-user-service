package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.domain.BanLevel
import com.faforever.userservice.backend.ucp.UcpBanHistoryEntry
import com.faforever.userservice.backend.ucp.UcpBanHistoryService
import com.faforever.userservice.backend.ucp.UcpBanStatus
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.grid.Grid.SelectionMode
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Route(value = "/ucp/ban-history", layout = UcpLayout::class)
@PermitAll
class UcpBanHistoryView(
    private val ucpSessionService: UcpSessionService,
    private val ucpBanHistoryService: UcpBanHistoryService,
) : VerticalLayout(),
    BeforeEnterObserver {

    companion object {
        private const val MAX_BANS_BEFORE_SCROLL = 8
        private const val SCROLLABLE_GRID_HEIGHT = "28rem"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    private val activeBansTitle = H3()
    private val historyBansTitle = H3()
    private val activeBansGrid = createBansGrid(getTranslation("ucp.banHistory.noActiveBans"))
    private val historyBansGrid = createBansGrid(getTranslation("ucp.banHistory.noHistoryBans"))

    init {
        setPadding(true)
        add(
            H2(getTranslation("ucp.nav.banHistory")),
            activeBansTitle,
            activeBansGrid,
            historyBansTitle,
            historyBansGrid,
        )
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val bans = ucpBanHistoryService.getBanHistoryForUser(ucpSessionService.getCurrentUser().userId)
        val activeBans = bans.filter { it.status == UcpBanStatus.ACTIVE }
        val historyBans = bans.filter { it.status != UcpBanStatus.ACTIVE }

        activeBansTitle.text = getTranslation("ucp.banHistory.activeTitle", activeBans.size)
        historyBansTitle.text = getTranslation("ucp.banHistory.historyTitle", historyBans.size)
        setBans(activeBansGrid, activeBans)
        setBans(historyBansGrid, historyBans)
    }

    private fun createBansGrid(emptyStateText: String): Grid<UcpBanHistoryEntry> {
        val expandIcons = mutableMapOf<Int, Span>()
        return Grid(UcpBanHistoryEntry::class.java, false).apply {
            addClassName("ban-history-grid")
            setSelectionMode(SelectionMode.NONE)
            setDetailsVisibleOnClick(true)
            setWidthFull()
            setEmptyStateText(emptyStateText)

            addComponentColumn { ban ->
                Span().apply {
                    addClassName("ban-expand-icon")
                    expandIcons[ban.id] = this
                    element.setAttribute("expanded", isDetailsVisible(ban))
                }
            }
                .setHeader("")
                .setWidth("2.5rem")
                .setFlexGrow(0)

            addColumn { getLevelLabel(it.level) }
                .setHeader(getTranslation("ucp.banHistory.column.level"))
                .setAutoWidth(true)
                .setFlexGrow(0)
            addComponentColumn { ban ->
                Span(ban.reason).apply { addClassName("ban-reason") }
            }
                .setHeader(getTranslation("ucp.banHistory.column.reason"))
                .setFlexGrow(1)
            addColumn { getDurationLabel(it) }
                .setHeader(getTranslation("ucp.banHistory.column.duration"))
                .setAutoWidth(true)
                .setFlexGrow(0)
            addColumn { formatDateTime(it.createTime) }
                .setHeader(getTranslation("ucp.banHistory.column.dateIssued"))
                .setAutoWidth(true)
                .setFlexGrow(0)
            addComponentColumn { ban ->
                Span(getStatusLabel(ban.status)).apply {
                    addClassNames("ban-status-badge", statusClass(ban.status))
                }
            }
                .setHeader(getTranslation("ucp.banHistory.column.status"))
                .setAutoWidth(true)
                .setFlexGrow(0)

            columns.forEach { it.isSortable = false }
            setItemDetailsRenderer(ComponentRenderer(::createBanDetails))
            addItemClickListener { event ->
                expandIcons[event.item.id]?.element?.setAttribute("expanded", isDetailsVisible(event.item))
            }
        }
    }

    private fun createBanDetails(ban: UcpBanHistoryEntry): Component =
        VerticalLayout().apply {
            addClassNames("ban-details", statusClass(ban.status))
            isPadding = true
            isSpacing = false

            add(
                detailRow(
                    getTranslation("ucp.banHistory.details.issuedBy"),
                    formatModerator(ban.authorUsername, ban.authorId),
                ),
            )
            add(
                detailRow(
                    getTranslation("ucp.banHistory.details.ends"),
                    ban.expiresAt?.let(::formatDateTime)
                        ?: getTranslation("ucp.banHistory.duration.permanent"),
                ),
            )

            ban.revokeReason?.takeIf(String::isNotBlank)?.let {
                add(detailRow(getTranslation("ucp.banHistory.details.revokeReason"), it))
            }
            ban.revokeAuthorId?.let {
                add(
                    detailRow(
                        getTranslation("ucp.banHistory.details.revokedBy"),
                        formatModerator(ban.revokeAuthorUsername, it),
                    ),
                )
            }
            ban.revokeTime?.let {
                add(detailRow(getTranslation("ucp.banHistory.details.revokedAt"), formatDateTime(it)))
            }
        }

    private fun setBans(
        grid: Grid<UcpBanHistoryEntry>,
        bans: List<UcpBanHistoryEntry>,
    ) {
        grid.setItems(bans)

        if (bans.size > MAX_BANS_BEFORE_SCROLL) {
            grid.setAllRowsVisible(false)
            grid.setHeight(SCROLLABLE_GRID_HEIGHT)
        } else {
            grid.setHeight(null)
            grid.setAllRowsVisible(true)
        }
    }

    private fun detailRow(label: String, value: String): Component =
        Div(
            Span(label).apply { addClassName("ban-details-label") },
            Span(value).apply { addClassName("ban-details-value") },
        ).apply { addClassName("ban-details-row") }

    private fun statusClass(status: UcpBanStatus): String = when (status) {
        UcpBanStatus.ACTIVE -> "ban-status-active"
        UcpBanStatus.EXPIRED -> "ban-status-expired"
        UcpBanStatus.REVOKED -> "ban-status-revoked"
    }

    private fun getDurationLabel(ban: UcpBanHistoryEntry): String {
        val expiresAt = ban.expiresAt ?: return getTranslation("ucp.banHistory.duration.permanent")
        val days = max(1, Duration.between(ban.createTime, expiresAt).toDays())
        return getTranslation("ucp.banHistory.duration.temporary", days)
    }

    private fun formatDateTime(dateTime: OffsetDateTime): String = dateTime.format(dateFormatter)

    private fun formatModerator(username: String?, userId: Int): String =
        username?.let { "$it (ID: $userId)" } ?: "ID: $userId"

    private fun getLevelLabel(level: BanLevel): String = when (level) {
        BanLevel.GLOBAL -> getTranslation("ucp.banHistory.level.global")
        BanLevel.CHAT -> getTranslation("ucp.banHistory.level.chat")
        BanLevel.VAULT -> getTranslation("ucp.banHistory.level.vault")
    }

    private fun getStatusLabel(status: UcpBanStatus): String = when (status) {
        UcpBanStatus.ACTIVE -> getTranslation("ucp.banHistory.status.active")
        UcpBanStatus.EXPIRED -> getTranslation("ucp.banHistory.status.expired")
        UcpBanStatus.REVOKED -> getTranslation("ucp.banHistory.status.revoked")
    }
}
