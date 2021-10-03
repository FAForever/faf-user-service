package com.faforever.userservice.hydra

import com.fasterxml.jackson.annotation.JsonProperty
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
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
    val baseUrl: String,

    /**
     * If you run Ory Hydra behind a reverse proxy, you most probably have it configured to allow
     * TLS termination in the reverse proxy and unencrypted traffic from inside the private ip range.
     *
     * However, Ory Hydra only accepts these connections if there is a X-Forwarded-Proto: https header.
     *
     * Setting this flag to true causes all http calls to Ory Hydra to do the same.
     */
    val fakeTlsForwarding: Boolean,

    /**
     * If you run Ory Hydra with its self signed certificate behind a safe network and don't want to
     * add the certificates (usually because you consider the network to be safe anyway) you can
     * disable the TLS certificate trust check.
     *
     * Setting this flag to true causes all http calls to Ory Hydra to accept untrusted certificates.
     */
    val acceptUntrustedTlsCertificates: Boolean,
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
        private val LOG: Logger = LoggerFactory.getLogger(HydraService::class.java)
        private const val paramChallenge = "challenge"
    }

    private val webClient = webClientBuilder
        .baseUrl(hydraProperties.baseUrl)
        .apply {
            if (hydraProperties.fakeTlsForwarding) {
                LOG.info("Configure Hydra WebClient to use fake TLS forwarding")
                it.defaultHeader("X-Forwarded-Proto", "https")
            }

            if (hydraProperties.acceptUntrustedTlsCertificates) {
                LOG.info("Configure Hydra WebClient to accept untrusted certificates")
                val sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build()

                val httpClient = HttpClient.create()
                    .secure { sslProviderBuilder -> sslProviderBuilder.sslContext(sslContext) }

                it.clientConnector(ReactorClientHttpConnector(httpClient))
            }

            if (hydraProperties.fakeTlsForwarding && hydraProperties.acceptUntrustedTlsCertificates) {
                LOG.warn(
                    "You enabled fake TLS forwarding together with accepting untrusted certificates." +
                        "Enabling both flags together does not make sense (but will not cause any errors)."
                )
            }
        }
        .build()

    // requesting a handled challenge throws HTTP 410 - Gone
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

    // accepting login request more than once throws HTTP 409 - Conflict
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

    // accepting consent more than once does not cause an error
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

    // rejecting consent more than once does not cause an error
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

    fun revokeRefreshTokens(revokeRefreshTokensRequest: RevokeRefreshTokensRequest): Mono<RedirectResponse> {
        return webClient
            .delete()
            .uri {
                it.path("/oauth2/auth/sessions/consent")
                    .queryParam("all", revokeRefreshTokensRequest.all)
                    .queryParam("subject", revokeRefreshTokensRequest.subject)
                    .queryParam("client", revokeRefreshTokensRequest.client)
                    .build()
            }
            .retrieve()
            .bodyToMono(RedirectResponse::class.java)
    }
}
