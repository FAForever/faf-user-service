package com.faforever.userservice.backend.account

import java.time.OffsetDateTime

data class AccountDeletedEvent(
    val userId: Int,
    val username: String,
    val email: String,
    val occurredAt: OffsetDateTime = OffsetDateTime.now(),
)
