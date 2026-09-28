package com.faforever.userservice.backend.ucp

import com.fasterxml.jackson.annotation.JsonProperty

data class PlayerAvatarUpdateEvent(
    @JsonProperty("player_id") val playerId: Int,
    @JsonProperty("avatar_id") val avatarId: Int?,
)
