package com.faforever.userservice

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.theme.lumo.Lumo
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@StyleSheet("styles.css")
@StyleSheet(Lumo.STYLESHEET)
@ApplicationScoped
class AppConfig : AppShellConfigurator {
    fun onStart(@Observes event: StartupEvent) {
        System.setProperty("vaadin.copilot.enable", "false")
    }
}
