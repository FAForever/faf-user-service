package com.faforever.userservice.backend.hydra

import com.fasterxml.jackson.annotation.JsonProperty

data class RedirectResponse(
    @JsonProperty("redirect_to")
    val redirectTo: String,
)
