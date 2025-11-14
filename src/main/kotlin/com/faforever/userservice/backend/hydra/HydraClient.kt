package com.faforever.userservice.backend.hydra

import io.quarkus.rest.client.reactive.ClientExceptionMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.FormParam
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient
import sh.ory.hydra.model.AcceptOAuth2ConsentRequest
import sh.ory.hydra.model.AcceptOAuth2LoginRequest
import sh.ory.hydra.model.IntrospectedOAuth2Token
import sh.ory.hydra.model.OAuth2ConsentRequest
import sh.ory.hydra.model.OAuth2LoginRequest
import sh.ory.hydra.model.OAuth2RedirectTo
import sh.ory.hydra.model.RejectOAuth2Request

@Path("/admin")
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
    fun getLoginRequest(@QueryParam("login_challenge") @NotBlank challenge: String): OAuth2LoginRequest

    @GET
    @Path("/oauth2/auth/requests/consent")
    fun getConsentRequest(@QueryParam("consent_challenge") @NotBlank challenge: String): OAuth2ConsentRequest

    // accepting login request more than once throws HTTP 409 - Conflict
    @PUT
    @Path("/oauth2/auth/requests/login/accept")
    fun acceptLoginRequest(
        @QueryParam("login_challenge") @NotBlank challenge: String,
        acceptLoginRequest: AcceptOAuth2LoginRequest,
    ): OAuth2RedirectTo

    @PUT
    @Path("/oauth2/auth/requests/login/reject")
    fun rejectLoginRequest(
        @QueryParam("login_challenge") @NotBlank challenge: String,
        payload: RejectOAuth2Request,
    ): OAuth2RedirectTo

    // accepting consent more than once does not cause an error
    @PUT
    @Path("/oauth2/auth/requests/consent/accept")
    fun acceptConsentRequest(
        @QueryParam("consent_challenge") @NotBlank challenge: String,
        acceptConsentRequest: AcceptOAuth2ConsentRequest,
    ): OAuth2RedirectTo

    // rejecting consent more than once does not cause an error
    @PUT
    @Path("/oauth2/auth/requests/consent/reject")
    fun rejectConsentRequest(
        @QueryParam("consent_challenge") @NotBlank challenge: String,
        error: RejectOAuth2Request,
    ): OAuth2RedirectTo

    @DELETE
    @Path("/oauth2/auth/sessions/consent")
    fun revokeRefreshTokens(
        @QueryParam("subject") subject: String,
        @QueryParam("all") all: Boolean?,
        @QueryParam("client") client: String?,
    ): Response

    @POST
    @Path("/oauth2/introspect")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun introspectToken(
        @FormParam("token") @NotBlank token: String,
        @FormParam("scope") scope: String?,
    ): IntrospectedOAuth2Token
}

class GoneException(override val message: String?) : RuntimeException(message)
