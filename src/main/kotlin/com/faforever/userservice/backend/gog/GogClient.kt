package com.faforever.userservice.backend.gog

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogGame(
    val game: GogGameDetails? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogGameDetails(
    val id: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogGamesPage(
    val _embedded: GogEmbedded? = null,
    val pages: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogEmbedded(
    val items: List<GogGame> = emptyList(),
)

@Path("/")
@ApplicationScoped
@RegisterRestClient(configKey = "gog-api")
interface GogClient {

    @GET
    @Path("/u/{username}")
    fun getProfile(
        @PathParam("username")
        username: String,
    ): String

    @GET
    @Path("/u/{username}/games/stats")
    fun getGamesPage(
        @PathParam("username")
        username: String,
        @QueryParam("sort")
        sort: String,
        @QueryParam("order")
        order: String,
        @QueryParam("page")
        page: Int,
    ): GogGamesPage
}
