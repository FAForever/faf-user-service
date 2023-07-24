package com.faforever.userservice.backend.hydra

import io.quarkus.rest.client.reactive.ClientExceptionMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import sh.ory.hydra.model.*


@Path("/")
@ApplicationScoped
@RegisterRestClient(configKey = "faf-ory-hydra")
interface HydraClient {

    companion object {
        @JvmStatic
        @ClientExceptionMapper
        fun toException(response: Response): RuntimeException? {
            return when (response.status) {
                404 -> NoChallengeException()
                410 -> GoneException("The request has already been handled")
                else -> null
            }
        }
    }

    // requesting a handled challenge throws HTTP 410 - Gone
    @GET
    @Path("/oauth2/auth/requests/login")
    fun getLoginRequest(@QueryParam("login_challenge") @NotBlank challenge: String): LoginRequest

    @GET
    @Path("/oauth2/auth/requests/consent")
    fun getConsentRequest(@QueryParam("consent_challenge") @NotBlank challenge: String): ConsentRequest

    // accepting login request more than once throws HTTP 409 - Conflict
    @PUT
    @Path("/oauth2/auth/requests/login/accept")
    fun acceptLoginRequest(
        @QueryParam("login_challenge") @NotBlank challenge: String,
        acceptLoginRequest: AcceptLoginRequest
    ): RedirectResponse

    @PUT
    @Path("/oauth2/auth/requests/login/reject")
    fun rejectLoginRequest(
        @QueryParam("login_challenge") @NotBlank challenge: String,
        error: GenericError
    ): RedirectResponse

    // accepting consent more than once does not cause an error
    @PUT
    @Path("/oauth2/auth/requests/consent/accept")
    fun acceptConsentRequest(
            @QueryParam("consent_challenge") @NotBlank challenge: String,
            acceptConsentRequest: AcceptConsentRequest
    ): RedirectResponse

    // rejecting consent more than once does not cause an error
    @PUT
    @Path("/oauth2/auth/requests/consent/reject")
    fun rejectConsentRequest(
            @QueryParam("consent_challenge") @NotBlank challenge: String,
            error: GenericError
    ): RedirectResponse

    @DELETE
    @Path("/oauth2/auth/sessions/consent")
    fun revokeRefreshTokens(
            @QueryParam("subject") subject: String,
            @QueryParam("all") all: Boolean?,
            @QueryParam("client") client: String?,
    ): Response
}

class GoneException(override val message: String?) : RuntimeException(message)