package com.faforever.web

import com.faforever.config.FafProperties
import com.faforever.domain.LoginResult
import com.faforever.domain.UserService
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


@TemplateData
data class LoginData(
    val loginChallenge: String?,
    val loginFailed: Any?,
    val loginThrottled: Any?,
)

data class LoginForm(
    val challenge: String? = null,
    val usernameOrEmail: String? = null,
    val password: String? = null,
    val login: String? = null
)

@Path("/oauth2")
@Produces(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
class OAuthController(
    private val properties: FafProperties,
    private val userService: UserService,
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
        return Templates.loginView(LoginData(challenge, loginFailed, loginThrottled))
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
            reverseProxyIp
        } else {
            LOG.warn(
                "IP address from reverse proxy missing. Please make sure this service runs behind a reverse " +
                        "proxy. Falling back to remote address.",
            )
            request.remoteAddress()?.hostAddress().toString()
        }

        val loginResult = userService.login(challenge, usernameOrEmail, password, ip)
        LOG.debug("Login result is: {}", loginResult)

        return  when(loginResult) {
            is LoginResult.SuccessfulLogin -> RestResponse.seeOther(loginResult.redirectTo.uri)
            is LoginResult.UserBanned ->  TODO()
            is LoginResult.UserNoGameOwnership -> TODO()
            is LoginResult.LoginThrottlingActive -> ok(Templates.loginView(LoginData(challenge, false, true)))
            is LoginResult.UserOrCredentialsMismatch -> ok(Templates.loginView(LoginData(challenge,  true, false)))
            is LoginResult.TechnicalError -> TODO()
        }
    }
}