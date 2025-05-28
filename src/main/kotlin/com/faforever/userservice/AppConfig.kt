package com.faforever.userservice

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.component.page.Push
import com.vaadin.flow.shared.ui.Transport
import com.vaadin.flow.theme.Theme
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@Theme("faforever")
@ApplicationScoped
@Push(transport = Transport.WEBSOCKET)
class AppConfig : AppShellConfigurator {
    fun onStart(@Observes event: StartupEvent) {
        System.setProperty("vaadin.copilot.enable", "false")
    }
}
