package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.config.FafProperties
import com.vaadin.flow.server.VaadinRequest
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class VaadinIpService(private val fafProperties: FafProperties) {

    fun getRealIp(): IpAddress {
        val currentRequest = VaadinRequest.getCurrent()
        val realIp = currentRequest.getHeader(fafProperties.realIpHeader()) ?: currentRequest.remoteAddr
        return IpAddress(realIp)
    }
}
