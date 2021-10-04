package com.faforever.userservice.controller

import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.domain.LoginResult
import com.faforever.userservice.domain.LoginResult.LoginThrottlingActive
import com.faforever.userservice.domain.LoginResult.SuccessfulLogin
import com.faforever.userservice.domain.LoginResult.UserBanned
import com.faforever.userservice.domain.LoginResult.UserNoGameOwnership
import com.faforever.userservice.domain.LoginResult.UserOrCredentialsMismatch
import com.faforever.userservice.domain.UserService
import com.faforever.userservice.hydra.HydraService
import com.faforever.userservice.hydra.RevokeRefreshTokensRequest
import com.faforever.userservice.security.OAuthRole
import com.faforever.userservice.security.OAuthScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.reactive.result.view.Rendering
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime

@Controller
@RequestMapping(path = ["/oauth2"])
class OAuthController(
    private val userService: UserService,
    private val hydraService: HydraService,
    private val fafProperties: FafProperties,
) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(OAuthController::class.java)
        const val LOGIN_TECHNICAL_ERROR_ROUTE = "login/technicalError"
    }

    @GetMapping("/login")
    fun showLogin(
        request: ServerHttpRequest,
        @RequestParam("login_challenge") challenge: String,
        model: Model,
    ): Mono<Rendering> =
        hydraService.getLoginRequest(challenge)
            .flatMap {
                val loginFailed = request.queryParams.containsKey("login_failed")
                val loginThrottled = request.queryParams.containsKey("login_throttled")
                model.addAttribute("loginFailed", loginFailed)
                model.addAttribute("loginThrottled", loginThrottled)
                model.addAttribute("challenge", challenge)
                model.addAttribute("passwordResetUrl", fafProperties.passwordResetUrl)
                model.addAttribute("registerAccountUrl", fafProperties.registerAccountUrl)
                Rendering.view("oauth2/login").build().toMono()
            }
            .onErrorResume { error ->
                LOG.debug("Getting login request from Hydra failed for login challenge $challenge", error)
                Rendering.redirectTo(LOGIN_TECHNICAL_ERROR_ROUTE).build().toMono()
            }

    @PostMapping("/login")
    fun performLogin(
        serverWebExchange: ServerWebExchange,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Mono<Void> =
        serverWebExchange.formData.flatMap { form ->
            val challenge = checkNotNull(form["login_challenge"]?.first())
            val usernameOrEmail = checkNotNull(form["usernameOrEmail"]?.first())
            val password = checkNotNull(form["password"]?.first())

            val reverseProxyIp = request.headers.getFirst("X-Real-IP")
            val ip = if (reverseProxyIp != null) reverseProxyIp else {
                LOG.warn("IP address from reverse proxy missing. Please make sure you this service runs behind reverse proxy. Falling back to remote address.")
                request.remoteAddress?.address?.hostAddress.toString()
            }

            userService.login(challenge, usernameOrEmail, password, ip)
                .flatMap {
                    LOG.debug("Login result is: $it")

                    when (it) {
                        is SuccessfulLogin -> redirect(response, it.redirectTo)
                        is UserBanned -> redirect(response, it.redirectTo)
                        is UserNoGameOwnership -> redirect(response, it.redirectTo)
                        is LoginThrottlingActive -> redirect(
                            response,
                            UriComponentsBuilder.fromUri(request.uri)
                                .queryParam("login_challenge", challenge)
                                .queryParam("login_throttled")
                                .build()
                                .toUriString()
                        )
                        is UserOrCredentialsMismatch -> redirect(
                            response,
                            UriComponentsBuilder.fromUri(request.uri)
                                .queryParam("login_challenge", challenge)
                                .queryParam("login_failed")
                                .build()
                                .toUriString()
                        )
                        is LoginResult.TechnicalError -> redirectTechnicalError(response)
                    }
                }
        }

    @GetMapping("/consent")
    fun showConsent(
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
                Rendering.view("oauth2/consent").build()
            }
            .onErrorResume { error ->
                LOG.debug("Getting consent request from Hydra failed for consent challenge $challenge", error)
                Rendering.redirectTo(LOGIN_TECHNICAL_ERROR_ROUTE).build().toMono()
            }

    @PostMapping("/consent")
    fun decideConsent(
        serverWebExchange: ServerWebExchange,
        response: ServerHttpResponse,
    ): Mono<Void> =
        serverWebExchange.formData.flatMap { form ->
            val challenge = checkNotNull(form["consent_challenge"]?.first())
            val permitted = form["action"]?.first()?.lowercase() == "permit"

            userService.decideConsent(challenge, permitted)
        }.flatMap { redirectUrl ->
            redirect(response, redirectUrl)
        }.onErrorResume { error ->
            LOG.debug("Deciding consent failed", error)
            redirectTechnicalError(response)
        }

    @PostMapping("/revokeTokens")
    @PreAuthorize("hasRole('${OAuthRole.ADMIN_ACCOUNT_BAN}') and @scopeService.hasScope(authentication, '${OAuthScope.ADMINISTRATIVE_ACTION}')")
    fun revokeRefreshTokens(
        @RequestBody revokeRefreshTokensRequest: RevokeRefreshTokensRequest,
        response: ServerHttpResponse,
    ): Mono<Void> {
        LOG.info(
            "Revoking consent sessions for subject `{}` on client `{}`", revokeRefreshTokensRequest.subject,
            if (revokeRefreshTokensRequest.all == true || revokeRefreshTokensRequest.client == null) "all" else revokeRefreshTokensRequest.client
        )
        return hydraService.revokeRefreshTokens(revokeRefreshTokensRequest)
            .flatMap { redirect(response, it.redirectTo) }
    }

    @GetMapping("/banned")
    fun showBan(
        @RequestParam("reason", required = true) reason: String,
        @RequestParam("expiration") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) expiration: OffsetDateTime?,
        model: Model,
    ): Mono<Rendering> {
        model.addAttribute("permanentBan", expiration == null)
        model.addAttribute("banReason", reason)
        model.addAttribute("banExpiration", expiration)
        return Mono.just(Rendering.view("oauth2/banned").build())
    }

    @GetMapping("/gameVerificationFailed")
    fun showOwnershipVerification(
        model: Model,
    ): Mono<Rendering> {
        model.addAttribute("accountLink", fafProperties.accountLinkUrl)
        return Mono.just(Rendering.view("oauth2/gameVerificationFailed").build())
    }

    @GetMapping("/$LOGIN_TECHNICAL_ERROR_ROUTE")
    fun showLoginTechnicalError(
        model: Model,
    ): Mono<Rendering> =
        Rendering.view("oauth2/loginTechnicalError").build().toMono()

    private fun redirect(response: ServerHttpResponse, uriString: String) = response.apply {
        statusCode = HttpStatus.FOUND
        headers.location = URI.create(uriString)
    }.setComplete()

    private fun redirectTechnicalError(response: ServerHttpResponse) = redirect(
        response,
        UriComponentsBuilder.fromUriString(LOGIN_TECHNICAL_ERROR_ROUTE).build().toUriString()
    )
}
