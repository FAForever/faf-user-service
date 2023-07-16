package com.faforever.userservice.hydra

import io.smallrye.mutiny.Uni
import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import sh.ory.hydra.model.*

@RegisterRestClient(configKey = "faf-ory-hydra")
@Path("/")
interface HydraClient {

    // requesting a handled challenge throws HTTP 410 - Gone
    @GET
    @Path("/oauth2/auth/requests/login?login_challenge={challenge}")
    fun getLoginRequest(@PathParam("challenge") @NotBlank challenge: String): LoginRequest

    @GET
    @Path("/oauth2/auth/requests/consent?consent_challenge={challenge}")
    fun getConsentRequest(@PathParam("challenge") @NotBlank challenge: String): Uni<ConsentRequest>

    // accepting login request more than once throws HTTP 409 - Conflict
    @PUT
    @Path("/oauth2/auth/requests/login/accept?login_challenge={challenge}")
    fun acceptLoginRequest(
            @PathParam("challenge") @NotBlank challenge: String,
            acceptLoginRequest: AcceptLoginRequest
    ): RedirectResponse

    @PUT
    @Path("/oauth2/auth/requests/login/reject?login_challenge={challenge}")
    fun rejectLoginRequest(
            @PathParam("challenge") @NotBlank challenge: String,
            error: GenericError
    ): RedirectResponse
//
//    // accepting consent more than once does not cause an error
//    @PUT
//    @Path("/oauth2/auth/requests/consent/accept?consent_challenge={challenge}")
//    fun acceptConsentRequest(
//            @PathParam("challenge") @NotBlank challenge: String,
//            acceptConsentRequest: AcceptConsentRequest
//    ): RedirectResponse
//
//    // rejecting consent more than once does not cause an error
//    @PUT
//    @Path("/oauth2/auth/requests/consent/reject?consent_challenge={challenge}")
//    fun rejectConsentRequest(
//            @PathParam("challenge") @NotBlank challenge: String,
//            error: GenericError
//    ): RedirectResponse
//
//    @DELETE
//    @Path("/oauth2/auth/sessions/consent")
//    fun revokeRefreshTokens(
//            @QueryParam("subject") subject: String,
//            @QueryParam("all") all: Boolean?,
//            @QueryParam("client") client: String?,
//    ): Uni<Response>
}