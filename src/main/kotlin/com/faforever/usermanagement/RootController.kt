package com.faforever.usermanagement

import com.faforever.usermanagement.domain.LoginResult.FailedLogin
import com.faforever.usermanagement.domain.LoginResult.SuccessfulLogin
import com.faforever.usermanagement.domain.UserRepository
import com.faforever.usermanagement.domain.UserService
import com.faforever.usermanagement.hydra.HydraService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.crypto.password.PasswordEncoder
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
class RootController(
    private val userService: UserService,
    private val hydraService: HydraService,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(RootController::class.java)
    }

    @GetMapping("login")
    fun showLogin(
        request: ServerHttpRequest,
        @RequestParam("login_challenge") challenge: String,
        model: Model,
    ): Mono<Rendering> {
        val loginFailed = request.queryParams.containsKey("login_failed")
        model.addAttribute("loginFailed", loginFailed)
        model.addAttribute("challenge", challenge)
        return Mono.just(Rendering.view("login").build())
    }

    @PostMapping("login")
    fun performLogin(
        serverWebExchange: ServerWebExchange,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Mono<Void> =
        serverWebExchange.formData.flatMap { form ->
            val challenge = form["login_challenge"]?.first()
            val username = form["username"]?.first()
            val password = form["password"]?.first()

            checkNotNull(challenge)
            checkNotNull(username)
            checkNotNull(password)

            userService.login(challenge, username, password)
                .flatMap {
                    when (it) {
                        is SuccessfulLogin -> response.apply {
                            statusCode = HttpStatus.FOUND
                            headers.location = URI.create(it.redirectTo)
                        }.setComplete()

                        is FailedLogin -> response.apply {
                            statusCode = HttpStatus.FOUND
                            headers.location = UriComponentsBuilder.fromUri(request.uri)
                                .queryParam("login_challenge", challenge)
                                .queryParam("login_failed")
                                .build()
                                .toUri()
                        }.setComplete()
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
            .map {
                model.addAttribute("challenge", challenge)
                model.addAttribute("consentRequest", it)
                Rendering.view("consent").build()
            }

    @PostMapping("consent")
    fun decideConsent(
        serverWebExchange: ServerWebExchange,
        response: ServerHttpResponse,
    ): Mono<Void> =
        serverWebExchange.formData.flatMap { form ->
            val challenge = form["consent_challenge"]?.first()
            val permitted = form["action"]?.first()?.toLowerCase() == "permit"
            checkNotNull(challenge)

            if (permitted) {
                hydraService.acceptConsentRequest(challenge, AcceptConsentRequest())
            } else {
                hydraService.rejectConsentRequest(challenge, GenericError("scope_denied"))
            }
                .flatMap {
                    response.apply {
                        statusCode = HttpStatus.FOUND
                        headers.location = URI.create(it.redirectTo)
                    }.setComplete()
                }
        }
}
