package com.faforever.userservice.hydra

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import sh.ory.hydra.model.AcceptConsentRequest
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.ConsentRequest
import sh.ory.hydra.model.GenericError
import sh.ory.hydra.model.LoginRequest
import javax.validation.constraints.NotBlank

@ConfigurationProperties(prefix = "hydra")
@Validated
@ConstructorBinding
data class HydraProperties(
    @NotBlank
    val baseUrl: String
)

data class RedirectResponse(
    @JsonProperty("redirect_to")
    val redirectTo: String
)

@Component
class HydraService(
    hydraProperties: HydraProperties,
    webClientBuilder: WebClient.Builder,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(HydraService::class.java)
        private const val paramChallenge = "challenge"
    }

    private val webClient = webClientBuilder
        .baseUrl(hydraProperties.baseUrl)
        .build()

    fun getLoginRequest(challenge: String): Mono<LoginRequest> =
        webClient
            .get()
            .uri(
                "/oauth2/auth/requests/login?login_challenge={$paramChallenge}",
                mapOf(paramChallenge to challenge)
            )
            .retrieve()
            .bodyToMono(LoginRequest::class.java)

    fun getConsentRequest(challenge: String): Mono<ConsentRequest> =
        webClient
            .get()
            .uri(
                "/oauth2/auth/requests/consent?consent_challenge={$paramChallenge}",
                mapOf(paramChallenge to challenge)
            )
            .retrieve()
            .bodyToMono(ConsentRequest::class.java)

    fun acceptLoginRequest(challenge: String, acceptLoginRequest: AcceptLoginRequest): Mono<RedirectResponse> =
        webClient
            .put()
            .uri(
                "/oauth2/auth/requests/login/accept?login_challenge={$paramChallenge}",
                mapOf(paramChallenge to challenge)
            )
            .bodyValue(acceptLoginRequest)
            .retrieve()
            .bodyToMono(RedirectResponse::class.java)

    fun rejectLoginRequest(challenge: String, error: GenericError): Mono<RedirectResponse> =
        webClient
            .put()
            .uri(
                "/oauth2/auth/requests/login/reject?login_challenge={$paramChallenge}",
                mapOf(paramChallenge to challenge)
            )
            .bodyValue(error)
            .retrieve()
            .bodyToMono(RedirectResponse::class.java)

    fun acceptConsentRequest(challenge: String, acceptConsentRequest: AcceptConsentRequest): Mono<RedirectResponse> =
        webClient
            .put()
            .uri(
                "/oauth2/auth/requests/consent/accept?consent_challenge={$paramChallenge}",
                mapOf(paramChallenge to challenge)
            )
            .bodyValue(acceptConsentRequest)
            .retrieve()
            .bodyToMono(RedirectResponse::class.java)

    fun rejectConsentRequest(challenge: String, error: GenericError): Mono<RedirectResponse> =
        webClient
            .put()
            .uri(
                "/oauth2/auth/requests/consent/reject?consent_challenge={$paramChallenge}",
                mapOf(paramChallenge to challenge)
            )
            .bodyValue(error)
            .retrieve()
            .bodyToMono(RedirectResponse::class.java)
}
