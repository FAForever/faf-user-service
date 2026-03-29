package com.faforever.userservice.web

import com.faforever.userservice.backend.altcha.AltchaChallenge
import com.faforever.userservice.backend.altcha.AltchaService
import jakarta.annotation.security.PermitAll
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/altcha")
@ApplicationScoped
class AltchaController(private val altchaService: AltchaService) {

    @GET
    @Path("/challenge")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    fun getChallenge(): AltchaChallenge = altchaService.createChallenge()
}
