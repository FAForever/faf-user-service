package com.faforever.userservice.backend.ucp

sealed interface LinkStatus {
    data class Linked(val identifier: String) : LinkStatus
    data object NotLinked : LinkStatus

    companion object {
        fun of(identifier: String?): LinkStatus =
            if (identifier.isNullOrBlank()) NotLinked else Linked(identifier)
    }
}
