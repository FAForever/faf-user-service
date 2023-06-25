package com.faforever.web

import com.faforever.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named

@ApplicationScoped
@Named("quteHelper")
class QuteHelper(
    val settings: FafProperties
) {
    val environment by lazy { settings.environment()?.let { "[$it]" ?: "" } }

    val isLoggedIn: Boolean get() = true

    val userName: String get() = "Brutus5000"
}