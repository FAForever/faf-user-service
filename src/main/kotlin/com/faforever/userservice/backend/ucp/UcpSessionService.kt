package com.faforever.userservice.backend.ucp

import com.vaadin.flow.server.VaadinSession
import jakarta.enterprise.context.ApplicationScoped
import java.security.Principal

data class UcpUser(
    val userId: Int,
    val userName: String,
) : Principal {
    override fun getName(): String = userName
}

@ApplicationScoped
class UcpSessionService {

    fun getCurrentUser(): UcpUser =
        getCurrentUserOrNull()
            ?: throw IllegalStateException("Unauthenticated UCP user in Vaadin session")

    fun getCurrentUserOrNull(): UcpUser? =
        VaadinSession.getCurrent()?.getAttribute(SESSION_ATTR) as? UcpUser

    fun setCurrentUser(user: UcpUser) {
        currentSession().setAttribute(SESSION_ATTR, user)
    }

    fun clear() {
        currentSession().setAttribute(SESSION_ATTR, null)
    }

    fun logout() {
        clear()
        currentSession().session.invalidate()
    }

    fun isLoggedIn(): Boolean = getCurrentUserOrNull() != null

    private fun currentSession(): VaadinSession =
        VaadinSession.getCurrent()
            ?: throw IllegalStateException("No VaadinSession is active for UCP session access")

    companion object {
        const val SESSION_ATTR = "ucp.currentUser"
    }
}
