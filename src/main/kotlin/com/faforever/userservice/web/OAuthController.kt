package com.faforever.userservice.web

import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.domain.ConsentForm
import com.faforever.userservice.domain.LoginForm
import com.faforever.userservice.domain.LoginResult
import com.faforever.userservice.domain.UserService
import com.faforever.userservice.domain.UserService.Companion.handleOryGoneRedirect
import com.faforever.userservice.hydra.HydraService
import com.faforever.userservice.hydra.RevokeRefreshTokensRequest
import com.faforever.userservice.security.FafRole
import com.faforever.userservice.security.OAuthScope
import com.faforever.userservice.security.RequiredRoleAndScope
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.annotation.RequestAttribute
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.views.ModelAndView
import jakarta.annotation.security.PermitAll
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import reactor.kotlin.core.publisher.toMono
import java.net.URI
import java.time.OffsetDateTime

private const val VIEW_LOGIN_ERROR = "oauth2/loginTechnicalError"

typealias GenericModelAndView = ModelAndView<Map<String, Any?>>
typealias HttpResponseWithModelView = MutableHttpResponse<GenericModelAndView>

@Controller("/oauth2")
open class OAuthController(
    private val userService: UserService,
    private val properties: FafProperties,
    private val hydraService: HydraService,
    private val viewFactory: ReactiveModelAndViewFactory
) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(OAuthController::class.java)
        const val PERMIT = "permit"
        const val DENY = "deny"
    }

    @Get("/login")
    @PermitAll
    open fun showLogin(
        @QueryValue("login_challenge") challenge: String,
        @QueryValue("loginFailed") loginFailed: Any?,
        @QueryValue("loginThrottled") loginThrottled: Any?,
        @RequestAttribute("_csrf") csrfToken: String
    ): Mono<HttpResponseWithModelView> = hydraService.getLoginRequest(challenge)
        .flatMap {
            viewFactory
                .with("_csrf", csrfToken)
                .with("passwordResetUrl", properties.passwordResetUrl)
                .with("registerAccountUrl", properties.registerAccountUrl)
                .with("loginFailed", loginFailed != null)
                .with("loginThrottled", loginThrottled != null)
                .with("loginForm", LoginForm(challenge = challenge))
                .build("oauth2/login")
        }.onErrorResume(HttpClientResponseException::class) { exception ->
            handleOryGoneRedirect(exception) { redirectTo ->
                LOG.debug("Login challenge $challenge was already solved, following Ory redirect")
                HttpResponse.redirect(URI.create(redirectTo))
            }
        }.onErrorResume { error ->
            LOG.error("Getting login request from Hydra failed for login challenge $challenge", error)
            viewFactory.buildError(VIEW_LOGIN_ERROR)
        }

    @Post("/login", consumes = [MediaType.APPLICATION_FORM_URLENCODED])
    @PermitAll
    fun performLogin(
        @Body loginForm: LoginForm,
        @Header("X-Real-Ip") reverseProxyIp: String?,
        @QueryValue("_csrf") csrfToken: String,
        request: HttpRequest<Any>
    ): Mono<HttpResponseWithModelView> {
        val ip = if (reverseProxyIp != null) reverseProxyIp else {
            LOG.warn(
                "IP address from reverse proxy missing. Please make sure this service runs behind a reverse " +
                    "proxy. Falling back to remote address."
            )
            request.remoteAddress.address?.hostAddress.toString()
        }

        return userService.login(loginForm.challenge!!, loginForm.usernameOrEmail!!, loginForm.password!!, ip)
            .flatMap {
                LOG.debug("Login result is: $it")

                when (it) {
                    is LoginResult.SuccessfulLogin -> redirect<GenericModelAndView>(it.redirectTo).toMono()
                    is LoginResult.UserBanned -> showBan(it.reason, it.expiresAt)
                    is LoginResult.UserNoGameOwnership -> showOwnershipVerification()
                    is LoginResult.LoginThrottlingActive -> showLogin(loginForm.challenge, null, true, csrfToken)
                    is LoginResult.UserOrCredentialsMismatch -> showLogin(loginForm.challenge, true, null, csrfToken)
                    is LoginResult.TechnicalError -> viewFactory.buildError(VIEW_LOGIN_ERROR)
                }
            }
    }

    @Get("/consent")
    @PermitAll
    fun showConsent(
        @QueryValue("consent_challenge") challenge: String,
        @RequestAttribute("_csrf") csrfToken: String
    ): Mono<HttpResponseWithModelView> =
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
            .flatMap { (consentRequest, user) ->
                viewFactory
                    .with("_csrf", csrfToken)
                    .with("denyForm", ConsentForm(challenge = challenge, action = DENY))
                    .with("permitForm", ConsentForm(challenge = challenge, action = PERMIT))
                    .with("consentRequest", consentRequest)
                    .with("client", consentRequest.client)
                    .with("user", user)
                    .build("oauth2/consent")
            }
            .onErrorResume { error ->
                LOG.error("Getting consent request from Hydra failed for consent challenge $challenge", error)
                viewFactory.buildError(VIEW_LOGIN_ERROR)
            }

    @Post("/consent", consumes = [MediaType.APPLICATION_FORM_URLENCODED])
    @PermitAll
    fun decideConsent(
        @Body consentForm: ConsentForm
    ): Mono<HttpResponseWithModelView> =
        userService.decideConsent(consentForm.challenge!!, consentForm.action == PERMIT)
            .flatMap { redirectUrl ->
                redirect<GenericModelAndView>(redirectUrl)
            }.onErrorResume { error ->
                LOG.error("Deciding consent failed", error)
                viewFactory.buildError(VIEW_LOGIN_ERROR)
            }

    @Post("/revokeTokens")
    @RequiredRoleAndScope(OAuthScope.ADMINISTRATIVE_ACTION, FafRole.ADMIN_ACCOUNT_BAN)
    fun revokeRefreshTokens(
        @Body revokeRefreshTokensRequest: RevokeRefreshTokensRequest
    ): Mono<Unit> {
        LOG.info(
            "Revoking consent sessions for subject `{}` on client `{}`",
            revokeRefreshTokensRequest.subject,
            if (revokeRefreshTokensRequest.all == true || revokeRefreshTokensRequest.client == null) "all"
            else revokeRefreshTokensRequest.client
        )
        return hydraService.revokeRefreshTokens(
            revokeRefreshTokensRequest.subject,
            revokeRefreshTokensRequest.all,
            revokeRefreshTokensRequest.client
        ).map {
            if (it.status != HttpStatus.NO_CONTENT) {
                LOG.error("Revoking tokens from Hydra failed for request: $revokeRefreshTokensRequest")
            }
        }
    }

    private fun showBan(reason: String, expiration: OffsetDateTime?): Mono<HttpResponseWithModelView> =
        viewFactory
            .with("permanentBan", expiration == null)
            .with("banReason", reason)
            .with("banExpiration", expiration)
            .build("oauth2/banned")

    private fun showOwnershipVerification(): Mono<HttpResponseWithModelView> =
        viewFactory
            .with("accountLink", properties.accountLinkUrl)
            .build("oauth2/gameVerificationFailed")

    private fun <T> redirect(uriString: String) = redirect<T>(URI.create(uriString))
    private fun <T> redirect(uri: URI) = HttpResponse.redirect<T>(uri).toMono()
}
