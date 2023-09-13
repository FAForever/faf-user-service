package com.faforever.userservice

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.theme.Theme
import jakarta.enterprise.context.ApplicationScoped

@Theme("faforever")
@ApplicationScoped
class AppConfig : AppShellConfigurator
