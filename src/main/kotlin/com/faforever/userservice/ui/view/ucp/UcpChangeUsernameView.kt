package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.backend.ucp.UcpUser
import com.faforever.userservice.backend.ucp.UcpUsernameService
import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/username", layout = UcpLayout::class)
@PermitAll
class UcpChangeUsernameView(
    private val ucpUsernameService: UcpUsernameService,
    private val ucpSessionService: UcpSessionService,
) : VerticalLayout() {

    private val newUsernameField = TextField().apply {
        label = getTranslation("ucp.username.newUsername")
        setWidthFull()
    }

    private val currentUsernameDisplay = Paragraph().apply {
        setWidthFull()
    }

    private val submitButton = Button(getTranslation("ucp.username.submit")) { handleChangeUsername() }.apply {
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
    }

    init {
        setPadding(true)
        setSizeFull()

        add(H2(getTranslation("ucp.username.title")))

        // Current username section
        currentUsernameDisplay.text = getTranslation(
            "ucp.username.current",
            ucpSessionService.getCurrentUser()?.userName ?: getTranslation("ucp.username.unknown"),
        )
        add(currentUsernameDisplay)

        // Form section
        add(newUsernameField)

        // Username rules
        add(
            Paragraph(getTranslation("ucp.username.rules")).apply {
                style.set("font-size", "var(--lumo-font-size-s)")
                style.set("color", "var(--lumo-secondary-text-color)")
                style.set("white-space", "pre-wrap")
            },
        )

        add(submitButton)
    }

    private fun handleChangeUsername() {
        val currentUser = ucpSessionService.getCurrentUser()
        val newUsername = newUsernameField.value
        val result = try {
            ucpUsernameService.changeUsername(currentUser, newUsername)
        } catch (exception: Exception) {
            Notification.show(
                getTranslation("ucp.username.error.updateFailed"),
                3000,
                Notification.Position.TOP_CENTER,
            ).addThemeVariants(NotificationVariant.LUMO_ERROR)
            return
        }

        when (result) {
            is UcpUsernameService.UsernameChangeResult.Success -> {
                ucpSessionService.setCurrentUser(UcpUser(result.userId, result.newUsername))
                Notification.show(getTranslation("ucp.username.success"), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS)
                newUsernameField.clear()
                // Update the current username display in-place to avoid heavy navigation
                currentUsernameDisplay.text = getTranslation("ucp.username.current", result.newUsername)
            }
            is UcpUsernameService.UsernameChangeResult.ValidationError -> {
                Notification.show(getTranslation(result.message), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
            UcpUsernameService.UsernameChangeResult.NotLoggedIn -> {
                Notification.show(
                    getTranslation("ucp.username.error.notLoggedIn"),
                    3000,
                    Notification.Position.TOP_CENTER,
                )
                    .addThemeVariants(NotificationVariant.LUMO_ERROR)
            }
        }
    }
}
