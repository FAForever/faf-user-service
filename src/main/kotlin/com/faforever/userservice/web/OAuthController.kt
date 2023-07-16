package com.faforever.userservice.web

import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.domain.IpAddress
import com.faforever.userservice.domain.LoginResult
import com.faforever.userservice.hydra.HydraService
import io.quarkus.qute.TemplateData
import io.quarkus.qute.TemplateInstance
import io.vertx.core.http.HttpServerRequest
import jakarta.annotation.security.PermitAll
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import org.jboss.resteasy.reactive.RestForm
import org.jboss.resteasy.reactive.RestResponse
import org.jboss.resteasy.reactive.RestResponse.ok
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.*


@TemplateData
data class LoginData(
    val loginChallenge: String?,
    val loginFailed: Any?,
    val loginThrottled: Any?,
)

@TemplateData
data class BanData(
    val reason: String,
    val permanent: Boolean,
    val expiresAt: OffsetDateTime?,
)

@Path("/oauth2")
@Produces(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
class OAuthController(
        private val properties: FafProperties,
        private val hydraService: HydraService,
) {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(OAuthController::class.java)
        const val PERMIT = "permit"
        const val DENY = "deny"
    }

    @GET
    @Path("/login")
    @PermitAll
    fun showLogin(
        @QueryParam("login_challenge") challenge: String?,
        @QueryParam("loginFailed") loginFailed: Boolean?,
        @QueryParam("loginThrottled") loginThrottled: Boolean?,
    ): TemplateInstance {
        return Templates.login(LoginData(challenge, loginFailed, loginThrottled))
    }

    @POST
    @Path("/login")
    @PermitAll
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun performLogin(
        @RestForm challenge: String,
        @RestForm usernameOrEmail: String,
        @RestForm password: String,
        @Context request: HttpServerRequest,
    ): RestResponse<TemplateInstance> {
        val reverseProxyIp = request.headers().get(properties.realIpHeader())
        val ip = if (reverseProxyIp != null) {
            IpAddress(reverseProxyIp)
        } else {
            LOG.warn(
                "IP address from reverse proxy missing. Please make sure this service runs behind a reverse " +
                        "proxy. Falling back to remote address.",
            )
            IpAddress(request.remoteAddress()?.hostAddress().toString())
        }

        val (redirectTo, loginResult) = hydraService.login(challenge, usernameOrEmail, password, ip)
        LOG.debug("Login result is: {}", loginResult)

        return when(loginResult) {
            is LoginResult.SuccessfulLogin -> RestResponse.seeOther(redirectTo.uri)
            is LoginResult.UserBanned ->  ok(Templates.banned(BanData(loginResult.reason, loginResult.expiresAt == null, loginResult.expiresAt)))
            is LoginResult.UserNoGameOwnership -> ok(Templates.gameVerificationFailed(properties.accountLinkUrl()))
            is LoginResult.LoginThrottlingActive -> ok(Templates.login(LoginData(challenge, false, true)))
            is LoginResult.UserOrCredentialsMismatch -> ok(Templates.login(LoginData(challenge, true, false)))
            is LoginResult.TechnicalError -> {
                val traceId = UUID.randomUUID().toString()
                LOG.warn("Technical error encountered. TraceId: $traceId")
                ok(Templates.loginTechnicalError(traceId))
            }
        }
    }
}