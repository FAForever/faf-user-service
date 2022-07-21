package com.faforever.userservice.domain

import io.micronaut.core.annotation.Introspected

@Introspected
data class LoginForm(
    val challenge: String? = null,
    val usernameOrEmail: String? = null,
    val password: String? = null
)

@Introspected
data class ConsentForm(
    val challenge: String? = null,
    val action: String? = null
)
