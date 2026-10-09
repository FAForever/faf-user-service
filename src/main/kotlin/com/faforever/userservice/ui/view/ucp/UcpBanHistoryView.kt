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
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.data.renderer.LitRenderer
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
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
        private const val MAX_GRID_HEIGHT = "28rem"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private var userTimeZone: ZoneId = ZoneId.systemDefault()

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

        setAlignSelf(
            FlexComponent.Alignment.START,
            activeBansTitle,
            activeBansGrid,
            historyBansTitle,
            historyBansGrid,
        )
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val bans = ucpBanHistoryService.getBanHistoryForUser(ucpSessionService.getCurrentUser().userId)

        val details = event.ui.page.extendedClientDetails

        userTimeZone = details.timeZoneId
            ?.let(ZoneId::of)
            ?: ZoneId.systemDefault()

        val activeBans = bans.filter { it.status == UcpBanStatus.ACTIVE }
        val historyBans = bans.filter { it.status != UcpBanStatus.ACTIVE }

        activeBansTitle.text = getTranslation("ucp.banHistory.activeTitle", activeBans.size)
        historyBansTitle.text = getTranslation("ucp.banHistory.historyTitle", historyBans.size)

        activeBansGrid.setItems(activeBans)
        historyBansGrid.setItems(historyBans)
    }

    private fun createBansGrid(emptyStateText: String): Grid<UcpBanHistoryEntry> =
        Grid(UcpBanHistoryEntry::class.java, false).apply {
            addClassName("ban-history-grid")
            setSelectionMode(SelectionMode.NONE)

            setDetailsVisibleOnClick(true)
            width = "100%"
            maxWidth = "70rem"
            setAllRowsVisible(true)
            setMaxHeight(MAX_GRID_HEIGHT)
            setEmptyStateText(emptyStateText)

            addColumn(
                LitRenderer.of(
                    """
                    <span class="ban-expand-button">
                        <vaadin-icon
                            .icon="${'$'}{model.detailsOpened ? 'vaadin:angle-down' : 'vaadin:angle-right'}">
                        </vaadin-icon>
                    </span>
                    """.trimIndent(),
                ),
            )
                .setHeader("")
                .setWidth("5rem")
                .setFlexGrow(0)

            addColumn { getLevelLabel(it.level) }
                .setHeader(getTranslation("ucp.banHistory.column.level"))
                .setWidth("5rem")
                .setFlexGrow(0)

            addComponentColumn { ban ->
                Span(truncateReason(ban.reason)).apply {
                    addClassName("ban-reason")
                }
            }
                .setHeader(getTranslation("ucp.banHistory.column.reason"))
                .setWidth("22rem")
                .setFlexGrow(0)

            addColumn { getDurationLabel(it) }
                .setHeader(getTranslation("ucp.banHistory.column.duration"))
                .setWidth("14rem")
                .setFlexGrow(0)

            addColumn { formatDateTime(it.createTime) }
                .setHeader(getTranslation("ucp.banHistory.column.dateIssued"))
                .setWidth("11rem")
                .setFlexGrow(0)

            addComponentColumn { ban ->
                Span(getStatusLabel(ban.status)).apply {
                    addClassNames(
                        "ban-status-badge",
                        statusClass(ban.status),
                    )
                }
            }
                .setHeader(getTranslation("ucp.banHistory.column.status"))
                .setWidth("8rem")
                .setFlexGrow(0)

            columns.forEach { it.isSortable = false }

            setItemDetailsRenderer(ComponentRenderer(::createBanDetails))
        }

    private fun createBanDetails(ban: UcpBanHistoryEntry): Component =
        VerticalLayout().apply {
            addClassNames("ban-details", statusClass(ban.status))
            isPadding = true
            isSpacing = false

            add(
                detailRow(
                    getTranslation("ucp.banHistory.details.fullReason"),
                    ban.reason,
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
                add(
                    detailRow(
                        getTranslation("ucp.banHistory.details.revokeReason"),
                        it,
                    ),
                )
            }

            ban.revokeTime?.let {
                add(
                    detailRow(
                        getTranslation("ucp.banHistory.details.revokedAt"),
                        formatDateTime(it),
                    ),
                )
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

    private fun truncateReason(reason: String, maxLength: Int = 60): String =
        if (reason.length > maxLength) {
            reason.take(maxLength).trimEnd() + "..."
        } else {
            reason
        }

    private fun getDurationLabel(ban: UcpBanHistoryEntry): String {
        val expiresAt = ban.expiresAt ?: return getTranslation("ucp.banHistory.duration.permanent")
        val days = max(1, Duration.between(ban.createTime, expiresAt).toDays())
        return getTranslation("ucp.banHistory.duration.temporary", days)
    }

    private fun formatDateTime(dateTime: OffsetDateTime): String =
        dateTime
            .atZoneSameInstant(userTimeZone)
            .format(dateFormatter)

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
