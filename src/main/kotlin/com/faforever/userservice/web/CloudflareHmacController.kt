package com.faforever.userservice.web

import com.faforever.userservice.backend.cloudflare.CloudflareService
import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.backend.security.OAuthScope
import com.faforever.userservice.config.FafProperties
import io.quarkus.security.PermissionsAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.UriBuilder
import java.net.URI

@Path("")
@ApplicationScoped
class CloudflareHmacController(
    val cloudflareService: CloudflareService,
    val hmacService: HmacService,
    val fafProperties: FafProperties,
) {

    data class HmacAccess(
        val accessUrl: URI,
    )

    data class HmacToken(
        val token: String,
    )

    @GET
    @Path("/lobby/access")
    @PermissionsAllowed("${FafRole.USER}:${OAuthScope.LOBBY}")
    fun getLobbyAccess(): HmacAccess {
        val lobby = fafProperties.lobby()
        val accessUri = lobby.accessUri()
        val token = cloudflareService.generateCloudFlareHmacToken(
            accessUri,
            lobby.secret(),
        )

        val accessUrl = UriBuilder.fromUri(accessUri).queryParam(lobby.accessParam(), token).build()

        return HmacAccess(accessUrl)
    }

    @GET
    @Path("/replay/access")
    @PermissionsAllowed("${FafRole.USER}:${OAuthScope.LOBBY}")
    fun getReplayAccess(): HmacAccess {
        val replay = fafProperties.replay()
        val accessUri = replay.accessUri()
        val token = cloudflareService.generateCloudFlareHmacToken(
            accessUri,
            replay.secret(),
        )

        val accessUrl = UriBuilder.fromUri(accessUri).queryParam(replay.accessParam(), token).build()

        return HmacAccess(accessUrl)
    }

    @GET
    @Path("/chat/access")
    @PermissionsAllowed("${FafRole.USER}:${OAuthScope.LOBBY}")
    fun getChatAccess(): HmacAccess {
        val chat = fafProperties.chat()
        val accessUri = chat.accessUri()
        val token = cloudflareService.generateCloudFlareHmacToken(
            accessUri,
            chat.secret(),
        )

        val accessUrl = UriBuilder.fromUri(accessUri).queryParam(chat.accessParam(), token).build()

        return HmacAccess(accessUrl)
    }

    @GET
    @Path("/challenge/token")
    @PermissionsAllowed(FafRole.USER)
    fun getGeneralHmacToken(): HmacToken {
        val hmac = fafProperties.jwt().hmac()?.let {
            hmacService.generateHmacToken(it.message(), it.secret())
        } ?: ""

        return HmacToken(hmac)
    }
}
