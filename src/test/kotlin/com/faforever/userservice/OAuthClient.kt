//package com.faforever.userservice
//
//import com.faforever.userservice.domain.ConsentForm
//import com.faforever.userservice.domain.LoginForm
//import com.faforever.userservice.backend.hydra.RevokeRefreshTokensRequest
//import io.micronaut.http.HttpHeaders
//import io.micronaut.http.HttpResponse
//import io.micronaut.http.MediaType
//import io.micronaut.http.annotation.Body
//import io.micronaut.http.annotation.Get
//import io.micronaut.http.annotation.Header
//import io.micronaut.http.annotation.Post
//import io.micronaut.http.annotation.QueryValue
//import io.micronaut.http.client.annotation.Client
//import reactor.core.publisher.Mono
//
//@Client("/oauth2")
//@Header(name = HttpHeaders.ACCEPT_LANGUAGE, value = "en-US")
//interface OAuthClient {
//    @Post("/login", produces = [MediaType.APPLICATION_FORM_URLENCODED])
//    fun postLoginRequest(
//        @QueryValue("_csrf") csrfToken: String,
//        @Body loginForm: LoginForm,
//    ): Mono<HttpResponse<String>>
//
//    @Get("/consent", produces = [MediaType.APPLICATION_FORM_URLENCODED])
//    fun getConsentRequest(
//        @QueryValue("consent_challenge") challenge: String,
//    ): Mono<HttpResponse<String>>
//
//    @Post("/consent", produces = [MediaType.APPLICATION_FORM_URLENCODED])
//    fun postConsentRequest(
//        @QueryValue("_csrf") csrfToken: String,
//        @Body challengeForm: ConsentForm,
//    ): Mono<HttpResponse<String>>
//
//    @Post("/revokeTokens")
//    fun revokeTokens(
//        @Body revokeRefreshTokensRequest: RevokeRefreshTokensRequest,
//    ): Mono<Unit>
//}
