package com.faforever.userservice.backend.hydra

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 * @param subject Subject is the user ID of the end-user that authenticated.
 * @param client Client to revoke consent sessions for.
 * @param all Revoke consent for all clients.
 */
data class RevokeRefreshTokensRequest(
    /* Subject is the user ID of the user to revoke consent sessions for. */
    @field:JsonProperty("subject")
    val subject: String,
    /* Client to revoke consent sessions for. */
    @field:JsonProperty("client")
    val client: String? = null,
    /* Revoke consent for all clients. */
    @field:JsonProperty("all")
    val all: Boolean? = null,
)
