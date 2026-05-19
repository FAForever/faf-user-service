package com.faforever.userservice.ui.security

import com.faforever.userservice.backend.ucp.UcpSessionService
import com.faforever.userservice.backend.ucp.UcpUser
import com.faforever.userservice.ui.view.ucp.UcpLoginView
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.server.ServiceInitEvent
import com.vaadin.flow.server.VaadinRequest
import com.vaadin.flow.server.VaadinRequestInterceptor
import com.vaadin.flow.server.VaadinResponse
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.server.VaadinServletRequest
import com.vaadin.flow.server.VaadinSession
import com.vaadin.flow.server.auth.NavigationAccessControl
import java.security.Principal

class UcpNavigationAccessControlInitializer : VaadinServiceInitListener {
    private val accessControl =
        UcpNavigationAccessControl().apply {
            setLoginView(UcpLoginView::class.java)
        }

    override fun serviceInit(serviceInitEvent: ServiceInitEvent) {
        serviceInitEvent.addVaadinRequestInterceptor(UcpPrincipalRequestInterceptor)
        serviceInitEvent.source.addUIInitListener { uiInitEvent ->
            uiInitEvent.ui.addBeforeEnterListener(accessControl)
        }
    }
}

private class UcpNavigationAccessControl : NavigationAccessControl() {
    override fun beforeEnter(event: BeforeEnterEvent) {
        if (event.location.path == "ucp" || event.location.path.startsWith("ucp/")) {
            super.beforeEnter(event)
        }
    }
}

private object UcpPrincipalRequestInterceptor : VaadinRequestInterceptor {
    override fun requestStart(request: VaadinRequest, response: VaadinResponse) {
        if (request is VaadinServletRequest) {
            request.service.setCurrentInstances(UcpPrincipalVaadinServletRequest(request), response)
        }
    }

    override fun handleException(
        request: VaadinRequest,
        response: VaadinResponse,
        session: VaadinSession,
        exception: Exception,
    ) = Unit

    override fun requestEnd(request: VaadinRequest, response: VaadinResponse, session: VaadinSession) = Unit
}

private class UcpPrincipalVaadinServletRequest(
    delegate: VaadinServletRequest,
) : VaadinServletRequest(delegate.httpServletRequest, delegate.service) {
    override fun getUserPrincipal(): Principal? =
        getCurrentUcpUser() ?: super.getUserPrincipal()

    override fun getRemoteUser(): String? =
        userPrincipal?.name ?: super.getRemoteUser()

    override fun isUserInRole(role: String): Boolean =
        (role == UCP_USER_ROLE && getCurrentUcpUser() != null) || super.isUserInRole(role)

    private fun getCurrentUcpUser(): UcpUser? =
        VaadinSession.getCurrent()?.getAttribute(UcpSessionService.SESSION_ATTR) as? UcpUser

    companion object {
        private const val UCP_USER_ROLE = "UCP_USER"
    }
}
