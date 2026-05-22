package com.faforever.userservice

import com.vaadin.flow.component.dependency.StyleSheet
import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.theme.lumo.Lumo
import jakarta.enterprise.context.ApplicationScoped

@StyleSheet("styles.css")
@StyleSheet(Lumo.STYLESHEET)
@ApplicationScoped
class AppConfig : AppShellConfigurator {
}
