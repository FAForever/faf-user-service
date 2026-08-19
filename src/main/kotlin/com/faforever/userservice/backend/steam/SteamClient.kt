package com.faforever.userservice.backend.steam

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@JsonIgnoreProperties(ignoreUnknown = true)
data class SteamOwnedGamesResponse(
    val response: SteamOwnedGamesInnerResponse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SteamOwnedGamesInnerResponse(
    @JsonProperty("game_count") val gameCount: Int? = null,
)

@Path("/IPlayerService/GetOwnedGames/v0001")
@ApplicationScoped
@RegisterRestClient(configKey = "steam-api")
interface SteamClient {
    @GET
    fun getOwnedGames(
        @QueryParam("key") apiKey: String,
        @QueryParam("steamid") steamId: String,
        @QueryParam("appids_filter[0]") appId: String,
        @QueryParam("format") format: String,
    ): SteamOwnedGamesResponse
}
