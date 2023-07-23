package com.faforever.userservice.web

import com.faforever.userservice.backend.hydra.HydraClient
import com.faforever.userservice.backend.hydra.RevokeRefreshTokensRequest
import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.OAuthScope
import io.quarkus.security.PermissionsAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.SecurityContext
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Path("/oauth2")
@ApplicationScoped
class OAuthController(
    @RestClient private val hydraClient: HydraClient
) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(OAuthController::class.java)
    }

    @POST
    @Path("/revokeTokens")
    @PermissionsAllowed("${FafRole.ADMIN_ACCOUNT_BAN}:${OAuthScope.ADMINISTRATIVE_ACTION}")
    fun revokeRefreshTokens(
        revokeRefreshTokensRequest: RevokeRefreshTokensRequest,
        @Context securityContext: SecurityContext
    ) {
        if (revokeRefreshTokensRequest.all == null && revokeRefreshTokensRequest.client == null) {
            throw BadRequestException("All and client cannot both be null")
        }

        LOG.info(
            "Revoking consent sessions for subject `{}` on client `{}`",
            revokeRefreshTokensRequest.subject,
            if (revokeRefreshTokensRequest.all == true) {
                "all"
            } else revokeRefreshTokensRequest.client,
        )
        val response = hydraClient.revokeRefreshTokens(
            revokeRefreshTokensRequest.subject,
            revokeRefreshTokensRequest.all,
            revokeRefreshTokensRequest.client,
        )
        if (response.status != 204) {
            LOG.error("Revoking tokens from Hydra failed for request: $revokeRefreshTokensRequest")
        }
    }
}