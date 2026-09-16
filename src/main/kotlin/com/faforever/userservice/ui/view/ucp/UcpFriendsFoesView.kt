package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.domain.FriendOrFoeEntry
import com.faforever.userservice.backend.domain.SocialStatus
import com.faforever.userservice.backend.ucp.UcpFriendsFoesService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/friends-foes", layout = UcpLayout::class)
@PermitAll
class UcpFriendsFoesView(
    private val ucpSessionService: UcpSessionService,
    private val ucpFriendsFoesService: UcpFriendsFoesService,
) : VerticalLayout(),
    BeforeEnterObserver {

    companion object {
        private const val NOTIFICATION_DURATION_MS = 3000
        private const val AVATAR_WIDTH = "40px"
        private const val AVATAR_HEIGHT = "20px"
    }

    private val usernameField = TextField().apply {
        label = getTranslation("ucp.friendsFoes.usernameField")
    }

    private val addFriendButton = Button(getTranslation("ucp.friendsFoes.addFriend")) {
        handleAdd(SocialStatus.FRIEND)
    }.apply {
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
    }

    private val addFoeButton = Button(getTranslation("ucp.friendsFoes.addFoe")) {
        handleAdd(SocialStatus.FOE)
    }.apply {
        addThemeVariants(ButtonVariant.LUMO_ERROR)
    }

    private val friendsList = friendOrFoeList()
    private val foesList = friendOrFoeList()

    init {
        setPadding(true)
        setSizeFull()

        add(H2(getTranslation("ucp.nav.friendsFoes")))

        add(
            HorizontalLayout(usernameField, addFriendButton, addFoeButton).apply {
                setWidthFull()
                alignItems = FlexComponent.Alignment.BASELINE
                setFlexGrow(1.0, usernameField)
            },
        )

        val friendsSection = listSection("ucp.friendsFoes.friends.heading", friendsList)
        val foesSection = listSection("ucp.friendsFoes.foes.heading", foesList)
        val lists = HorizontalLayout(friendsSection, foesSection).apply {
            setWidthFull()
            isPadding = false
            isSpacing = true
            alignItems = FlexComponent.Alignment.STRETCH
            setFlexGrow(1.0, friendsSection, foesSection)
            style.set("flex-wrap", "wrap")
        }
        add(lists)
        expand(lists)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        refreshLists()
    }

    private fun listSection(headingKey: String, list: VerticalLayout): VerticalLayout =
        VerticalLayout(H3(getTranslation(headingKey)), list).apply {
            addClassName("friends-foes-section")
            isPadding = true
            isSpacing = true
            minWidth = "18rem"
            width = "0"
            expand(list)
        }

    private fun friendOrFoeList(): VerticalLayout =
        VerticalLayout().apply {
            addClassName("friends-foes-list")
            isPadding = false
            isSpacing = false
            setWidthFull()
        }

    private fun setListEntries(list: VerticalLayout, entries: List<FriendOrFoeEntry>, emptyStateKey: String) {
        list.removeAll()
        if (entries.isEmpty()) {
            list.add(Span(getTranslation(emptyStateKey)))
            return
        }
        entries.forEach { list.add(playerRow(it)) }
    }

    private fun playerRow(entry: FriendOrFoeEntry): Component {
        val name = Span(entry.username)
        val spacer = Span()
        val row = HorizontalLayout(name).apply {
            addClassName("friends-foes-row")
            setWidthFull()
            isPadding = false
            isSpacing = true
            alignItems = FlexComponent.Alignment.CENTER
        }
        if (!entry.avatarUrl.isNullOrBlank()) {
            val altText = entry.avatarTooltip?.takeIf { it.isNotBlank() } ?: entry.username
            row.add(
                Image(entry.avatarUrl, altText).apply {
                    width = AVATAR_WIDTH
                    height = AVATAR_HEIGHT
                    style.set("object-fit", "contain")
                    style.set("flex-shrink", "0")
                },
            )
        }
        row.add(spacer)
        row.setFlexGrow(1.0, spacer)
        row.add(
            Button(getTranslation("ucp.friendsFoes.remove")) { handleRemove(entry) }.apply {
                addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR)
            },
        )
        return row
    }

    private fun handleAdd(status: SocialStatus) {
        val currentUser = ucpSessionService.getCurrentUser()
        when (val result = ucpFriendsFoesService.add(currentUser.userId, usernameField.value, status)) {
            is UcpFriendsFoesService.AddResult.Success -> {
                val successKey = if (status == SocialStatus.FRIEND) {
                    "ucp.friendsFoes.success.addedFriend"
                } else {
                    "ucp.friendsFoes.success.addedFoe"
                }
                notify(getTranslation(successKey, result.entry.username), NotificationVariant.LUMO_SUCCESS)
                usernameField.clear()
                refreshLists()
            }
            is UcpFriendsFoesService.AddResult.ValidationError -> {
                notify(getTranslation(result.message), NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun handleRemove(entry: FriendOrFoeEntry) {
        val currentUser = ucpSessionService.getCurrentUser()
        when (ucpFriendsFoesService.remove(currentUser.userId, entry.subjectId)) {
            UcpFriendsFoesService.RemoveResult.Success -> {
                notify(
                    getTranslation("ucp.friendsFoes.success.removed", entry.username),
                    NotificationVariant.LUMO_SUCCESS,
                )
                refreshLists()
            }
            UcpFriendsFoesService.RemoveResult.NotFound -> {
                notify(getTranslation("ucp.friendsFoes.error.removeFailed"), NotificationVariant.LUMO_ERROR)
            }
        }
    }

    private fun refreshLists() {
        val currentUser = ucpSessionService.getCurrentUser()
        setListEntries(friendsList, ucpFriendsFoesService.getFriends(currentUser.userId), "ucp.friendsFoes.noFriends")
        setListEntries(foesList, ucpFriendsFoesService.getFoes(currentUser.userId), "ucp.friendsFoes.noFoes")
    }

    private fun notify(message: String, variant: NotificationVariant) {
        Notification.show(message, NOTIFICATION_DURATION_MS, Notification.Position.TOP_CENTER)
            .addThemeVariants(variant)
    }
}
