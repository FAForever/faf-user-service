package com.faforever.userservice.web

import com.faforever.userservice.backend.cloudflare.CloudflareService
import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.OAuthScope
import com.faforever.userservice.config.FafProperties
import io.quarkus.security.PermissionsAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.UriBuilder
import java.net.URI

@Path("/lobby")
@ApplicationScoped
class LobbyController(
    val cloudflareService: CloudflareService, val fafProperties: FafProperties
) {

    @GET
    @Path("/access")
    @PermissionsAllowed("${FafRole.USER}:${OAuthScope.LOBBY}")
    fun getLobbyAccess(): LobbyAccess {
        val lobby = fafProperties.lobby()
        val accessUri = lobby.accessUri()
        val token = cloudflareService.generateCloudFlareHmacToken(
            accessUri, lobby.secret()
        )

        val accessUrl = UriBuilder.fromUri(accessUri).queryParam(lobby.accessParam(), token).build()

        return LobbyAccess(accessUrl)
    }
}

data class LobbyAccess(
    val accessUrl: URI
)
