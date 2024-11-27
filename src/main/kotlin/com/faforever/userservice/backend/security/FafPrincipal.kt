package com.faforever.userservice.backend.security

import java.security.Principal

data class FafPrincipal(
    val userId: Int,
    val userName: String,
): Principal {
    override fun getName() = userName
}
