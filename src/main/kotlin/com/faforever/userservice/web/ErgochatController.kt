package com.faforever.userservice.web

import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import jakarta.annotation.security.RolesAllowed
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.JsonString
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.eclipse.microprofile.jwt.JsonWebToken
import java.util.*

@Path("/irc/ergochat")
@ApplicationScoped
class ErgochatController(
    private val properties: FafProperties,
    private val hmacService: HmacService,
) {
    data class LoginRequest(
        val accountName: String,
        val passphrase: String,
        val ip: String,
    )

    data class LoginResponse(
        val success: Boolean,
        val accountName: String,
        val error: String? = null,
    )

    data class IrcToken(
        val value: String,
    )

    @POST
    @Path("/login")
    fun authenticateChatUser(loginData: LoginRequest): LoginResponse {
        val (authenticationType, authenticationValue) = loginData.passphrase.split(":").let {
            if (it.size != 2) {
                // This will show up in the ergochat logs
                return LoginResponse(
                    success = false,
                    accountName = loginData.accountName,
                    error = "passphrase does not follow [type:password] specification",
                )
            }
            it[0] to it[1]
        }

        return when (authenticationType) {
            "token" -> authenticateToken(authenticationValue, loginData.accountName)
            "static" -> authenticateStatic(authenticationValue, loginData.accountName)
            else -> LoginResponse(
                success = false,
                accountName = loginData.accountName,
                error = "unknown authentication type $authenticationType",
            )
        }
    }

    private fun authenticateStatic(
        authenticationValue: String,
        accountName: String,
    ): LoginResponse {
        val success = properties.irc().fixedUsers().any { (user, password) ->
            user.equals(accountName, ignoreCase = true) && password == authenticationValue
        }

        return LoginResponse(
            success = success,
            accountName = accountName,
            error = if (success) {
                null
            } else {
                "Username or password does not match for static user $accountName"
            },
        )
    }

    private fun authenticateToken(
        authenticationValue: String,
        accountName: String,
    ): LoginResponse {
        val isValidHmac = hmacService.isValidHmacToken(
            authenticationValue,
            accountName.lowercase(Locale.ROOT),
            properties.irc().secret(),
            properties.irc().tokenTtl(),
        )

        return LoginResponse(
            success = isValidHmac,
            accountName = accountName,
            error = if (isValidHmac) {
                null
            } else {
                "Invalid token"
            },
        )
    }

    @GET
    @Path("/token")
    @RolesAllowed(FafRole.USER)
    fun getIrcToken(@Context context: SecurityContext): Response {
        return when (val principal = context.userPrincipal) {
            is JsonWebToken -> {
                return principal.claim<Map<String, Any>>("ext")
                    .map { it["username"] as JsonString }
                    .map { it.string }
                    .map { it.lowercase(Locale.ROOT) }
                    .map { hmacService.generateHmacToken(it, properties.irc().secret()) }
                    .map { Response.ok(IrcToken(it)).build() }
                    .orElse(Response.status(Response.Status.UNAUTHORIZED).build())
            }

            else -> Response.status(Response.Status.UNAUTHORIZED).build()
        }
    }
}
