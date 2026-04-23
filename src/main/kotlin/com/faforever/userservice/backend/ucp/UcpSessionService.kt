package com.faforever.userservice.backend.ucp

import com.vaadin.flow.server.VaadinSession
import jakarta.enterprise.context.ApplicationScoped

data class UcpUser(
    val userId: Int,
    val userName: String,
)

@ApplicationScoped
class UcpSessionService {

    fun getCurrentUser(): UcpUser? {
        val session = VaadinSession.getCurrent() ?: return null
        return session.getAttribute(SESSION_ATTR) as? UcpUser
    }

    fun setCurrentUser(user: UcpUser) {
        val session = VaadinSession.getCurrent() ?: return
        session.setAttribute(SESSION_ATTR, user)
    }

    fun clear() {
        VaadinSession.getCurrent()?.setAttribute(SESSION_ATTR, null)
    }

    fun isLoggedIn(): Boolean = getCurrentUser() != null

    companion object {
        private const val SESSION_ATTR = "ucp.currentUser"
    }
}
