package com.faforever.userservice.controller

import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.domain.LoginResult.LoginThrottlingActive
import com.faforever.userservice.domain.LoginResult.SuccessfulLogin
import com.faforever.userservice.domain.LoginResult.UserBanned
import com.faforever.userservice.domain.LoginResult.UserOrCredentialsMismatch
import com.faforever.userservice.domain.UserService
import com.faforever.userservice.hydra.HydraService
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.result.view.Rendering
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import sh.ory.hydra.model.AcceptConsentRequest
import sh.ory.hydra.model.GenericError
import java.net.URI

@Controller
class OAuthController(
    private val userService: UserService,
    private val hydraService: HydraService,
    private val fafProperties: FafProperties,
) {
    @GetMapping("login")
    fun showLogin(
        request: ServerHttpRequest,
        @RequestParam("login_challenge") challenge: String,
        model: Model,
    ): Mono<Rendering> {
        val loginFailed = request.queryParams.containsKey("login_failed")
        model.addAttribute("loginFailed", loginFailed)
        model.addAttribute("challenge", challenge)
        model.addAttribute("passwordResetUrl", fafProperties.passwordResetUrl)
        model.addAttribute("registerAccountUrl", fafProperties.registerAccountUrl)
        return Mono.just(Rendering.view("login").build())
    }

    private fun redirect(response: ServerHttpResponse, uriString: String) = response.apply {
        statusCode = HttpStatus.FOUND
        headers.location = URI.create(uriString)
    }.setComplete()

    @PostMapping("login")
    fun performLogin(
        serverWebExchange: ServerWebExchange,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Mono<Void> =
        serverWebExchange.formData.flatMap { form ->
            val challenge = checkNotNull(form["login_challenge"]?.first())
            val username = checkNotNull(form["username"]?.first())
            val password = checkNotNull(form["password"]?.first())
            // TODO: This does not work behind a proxy. Use forwarded IP address header instead
            val ip = request.remoteAddress?.address?.hostAddress.toString()

            userService.login(challenge, username, password, ip)
                .flatMap {
                    when (it) {
                        is SuccessfulLogin -> redirect(response, it.redirectTo)
                        is UserBanned -> redirect(response, it.redirectTo)
                        is LoginThrottlingActive -> redirect(response, it.redirectTo)
                        is UserOrCredentialsMismatch -> redirect(
                            response,
                            UriComponentsBuilder.fromUri(request.uri)
                                .queryParam("login_challenge", challenge)
                                .queryParam("login_failed")
                                .build()
                                .toUriString()
                        )
                    }
                }
        }

    @GetMapping("consent")
    fun showConsent(
        request: ServerHttpRequest,
        @RequestParam("consent_challenge", required = true) challenge: String,
        model: Model,
    ): Mono<Rendering> =
        hydraService.getConsentRequest(challenge)
            .flatMap { consentRequest ->
                if (consentRequest.subject == null) {
                    Mono.error(IllegalStateException("Subject missing"))
                } else {
                    userService.findUserBySubject(consentRequest.subject)
                        .map { consentRequest to it }
                        .switchIfEmpty(Mono.error(IllegalStateException("Subject missing")))
                }
            }
            .map { (consentRequest, user) ->
                model.addAttribute("challenge", challenge)
                model.addAttribute("consentRequest", consentRequest)
                model.addAttribute("client", consentRequest.client)
                model.addAttribute("user", user)
                Rendering.view("consent").build()
            }

    @PostMapping("consent")
    fun decideConsent(
        serverWebExchange: ServerWebExchange,
        response: ServerHttpResponse,
    ): Mono<Void> =
        serverWebExchange.formData.flatMap { form ->
            val challenge = checkNotNull(form["consent_challenge"]?.first())
            val permitted = form["action"]?.first()?.toLowerCase() == "permit"

            if (permitted) {
                hydraService.acceptConsentRequest(challenge, AcceptConsentRequest())
            } else {
                hydraService.rejectConsentRequest(challenge, GenericError("scope_denied"))
            }
                .flatMap {
                    redirect(response, it.redirectTo)
                }
        }
}
