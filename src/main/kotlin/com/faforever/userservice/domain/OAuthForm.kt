package com.faforever.userservice.domain

data class LoginForm(
    val challenge: String? = null,
    val usernameOrEmail: String? = null,
    val password: String? = null,
)

data class ConsentForm(
    val challenge: String? = null,
    val action: String? = null,
)
