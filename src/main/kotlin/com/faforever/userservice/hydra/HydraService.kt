package com.faforever.userservice.hydra

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client
import reactor.core.publisher.Mono
import sh.ory.hydra.model.AcceptConsentRequest
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.ConsentRequest
import sh.ory.hydra.model.GenericError
import sh.ory.hydra.model.LoginRequest
import javax.validation.constraints.NotBlank

@Client("\${faf.hydra-base-url}")
interface HydraService {

    // requesting a handled challenge throws HTTP 410 - Gone
    @Get("/oauth2/auth/requests/login?login_challenge={challenge}")
    fun getLoginRequest(@NotBlank challenge: String): Mono<LoginRequest>

    @Get("/oauth2/auth/requests/consent?consent_challenge={challenge}")
    fun getConsentRequest(@NotBlank challenge: String): Mono<ConsentRequest>

    // accepting login request more than once throws HTTP 409 - Conflict
    @Put("/oauth2/auth/requests/login/accept?login_challenge={challenge}")
    fun acceptLoginRequest(@NotBlank challenge: String, @Body acceptLoginRequest: AcceptLoginRequest): Mono<RedirectResponse>

    @Put("/oauth2/auth/requests/login/reject?login_challenge={challenge}")
    fun rejectLoginRequest(@NotBlank challenge: String, @Body error: GenericError): Mono<RedirectResponse>

    // accepting consent more than once does not cause an error
    @Put("/oauth2/auth/requests/consent/accept?consent_challenge={challenge}")
    fun acceptConsentRequest(@NotBlank challenge: String, @Body acceptConsentRequest: AcceptConsentRequest): Mono<RedirectResponse>

    // rejecting consent more than once does not cause an error
    @Put("/oauth2/auth/requests/consent/reject?consent_challenge={challenge}")
    fun rejectConsentRequest(@NotBlank challenge: String, @Body error: GenericError): Mono<RedirectResponse>

    @Delete("/oauth2/auth/sessions/consent")
    fun revokeRefreshTokens(
        @QueryValue subject: String,
        @QueryValue all: Boolean?,
        @QueryValue client: String?,
    ): Mono<HttpResponse<Unit>>
}
