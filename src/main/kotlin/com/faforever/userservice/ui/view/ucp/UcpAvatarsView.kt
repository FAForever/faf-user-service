package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.AvatarSelectionResult
import com.faforever.userservice.backend.ucp.UcpAccountDataService
import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.backend.ucp.UserAvatar
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/avatars", layout = UcpLayout::class)
@PermitAll
class UcpAvatarsView(
    private val ucpSessionService: UcpSessionService,
    private val ucpAccountDataService: UcpAccountDataService,
) : VerticalLayout(),
    BeforeEnterObserver {

    companion object {
        private const val AVATAR_WIDTH = "120px"
        private const val AVATAR_HEIGHT = "60px"
        private const val BORDER_SELECTED = "2px solid var(--lumo-primary-color)"
        private const val BORDER_UNSELECTED = "2px solid transparent"
    }

    private var contentRow: VerticalLayout? = null
    private var currentAvatarDisplay: VerticalLayout? = null
    private val avatarContainers = mutableMapOf<Int?, VerticalLayout>()

    init {
        setPadding(true)
        setSizeFull()
        add(H2(getTranslation("ucp.nav.avatars")))
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = ucpSessionService.getCurrentUser()
        buildContent(user.userId)
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

    private fun buildCurrentAvatarDisplay(avatar: UserAvatar?): VerticalLayout =
        VerticalLayout().apply {
            isPadding = false
            isSpacing = false
            alignItems = FlexComponent.Alignment.CENTER
            style.set("margin-bottom", "16px")
            style.set("min-height", "100px")

            if (avatar == null) {
                val spacer = Span().apply {
                    style.set("display", "inline-block")
                    style.set("width", AVATAR_WIDTH)
                    style.set("height", AVATAR_HEIGHT)
                    style.set("visibility", "hidden")
                }
                add(spacer)
                val noAvatarLabel = Span(getTranslation("ucp.accountData.noAvatar")).apply {
                    style.set("font-size", "12px")
                    style.set("color", "var(--lumo-secondary-text-color)")
                }
                add(noAvatarLabel)
            } else {
                add(buildAvatarContent(avatar.url, avatar.tooltip))
                val tooltipLabel = Span(avatar.tooltip ?: "Avatar").apply {
                    style.set("font-size", "12px")
                    style.set("text-align", "center")
                }
                add(tooltipLabel)
            }
        }

    private fun updateCurrentAvatarDisplay(avatar: UserAvatar?) {
        val newDisplay = buildCurrentAvatarDisplay(avatar)
        currentAvatarDisplay?.let { contentRow?.replace(it, newDisplay) }
        currentAvatarDisplay = newDisplay
    }

    private fun updateSelectionBorders(selectedId: Int?) {
        avatarContainers.forEach { (id, container) ->
            container.style.set(
                "border",
                if (id == selectedId) BORDER_SELECTED else BORDER_UNSELECTED,
            )
        }
    }

    private fun buildAvatarContainer(
        content: Component,
        labelText: String,
        isSelected: Boolean,
        avatarId: Int?,
        onClick: () -> Unit,
    ): VerticalLayout {
        val avatarButton = Button(content).apply {
            addThemeVariants(ButtonVariant.LUMO_TERTIARY)
            style.set("width", AVATAR_WIDTH)
            style.set("height", AVATAR_HEIGHT)
            style.set("padding", "0")
            style.set("box-sizing", "border-box")
            addClickListener { onClick() }
        }

        val label = Span(labelText).apply {
            style.set("margin-top", "4px")
            style.set("font-size", "12px")
            style.set("text-align", "center")
        }

        return VerticalLayout().apply {
            isPadding = false
            isSpacing = false
            alignItems = FlexComponent.Alignment.CENTER
            width = "140px"
            style.set("min-height", "100px")
            style.set("border-radius", "4px")
            style.set("padding", "4px")
            style.set("box-sizing", "border-box")
            style.set("border", if (isSelected) BORDER_SELECTED else BORDER_UNSELECTED)
            add(avatarButton, label)
        }.also { avatarContainers[avatarId] = it }
    }

    private fun buildContent(userId: Int) {
        avatarContainers.clear()
        val availableAvatars = ucpAccountDataService.getAvailableAvatars(userId)
        val equippedAvatar = availableAvatars.firstOrNull { it.isSelected }

        val layout = VerticalLayout().apply {
            isPadding = false
            isSpacing = true
        }

        if (availableAvatars.isEmpty()) {
            layout.add(Span(getTranslation("ucp.accountData.noAvatarsAvailable")))
            contentRow?.let { remove(it) }
            contentRow = layout
            add(layout)
            return
        }

        val currentDisplay = buildCurrentAvatarDisplay(equippedAvatar)
        currentAvatarDisplay = currentDisplay
        layout.add(currentDisplay)

        val avatarLayout = HorizontalLayout().apply {
            isPadding = true
            isSpacing = true
            isWrap = true
        }

        avatarLayout.add(
            buildAvatarContainer(
                content = buildAvatarContent(null, null),
                labelText = getTranslation("ucp.accountData.noAvatarButtonLabel"),
                isSelected = equippedAvatar == null,
                avatarId = null,
                onClick = {
                    ucpAccountDataService.deselectAvatar(userId)
                    updateCurrentAvatarDisplay(null)
                    updateSelectionBorders(null)
                },
            ),
        )

        availableAvatars.forEach { avatar ->
            avatarLayout.add(
                buildAvatarContainer(
                    content = buildAvatarContent(avatar.url, avatar.tooltip),
                    labelText = avatar.tooltip ?: "Avatar",
                    isSelected = avatar.isSelected,
                    avatarId = avatar.id,
                    onClick = {
                        when (ucpAccountDataService.selectAvatar(userId, avatar.id)) {
                            AvatarSelectionResult.Success -> {
                                updateCurrentAvatarDisplay(avatar)
                                updateSelectionBorders(avatar.id)
                            }
                            AvatarSelectionResult.AvatarExpired -> {
                                Notification.show(
                                    getTranslation("ucp.avatars.avatarExpired"),
                                    5000,
                                    Notification.Position.MIDDLE,
                                ).addThemeVariants(NotificationVariant.LUMO_ERROR)
                                buildContent(userId)
                            }
                        }
                    },
                ),
            )
        }

        layout.add(avatarLayout)
        contentRow?.let { remove(it) }
        contentRow = layout
        add(layout)
    }
}
