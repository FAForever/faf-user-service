package com.faforever.userservice.web

import com.faforever.userservice.backend.hydra.HydraClient
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import org.eclipse.microprofile.rest.client.inject.RestClient

@Path("/irc/ergochat")
@ApplicationScoped
class ErgochatController(
    private val properties: FafProperties,
    @RestClient private val hydraClient: HydraClient,
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
            "oauth" -> {
                val tokenIntrospection = hydraClient.introspectToken(authenticationValue, null)
                if (!tokenIntrospection.active) {
                    LoginResponse(
                        success = false,
                        accountName = loginData.accountName,
                        error = "Invalid token",
                    )
                } else if ((tokenIntrospection.ext as Map<*, *>)["username"] != loginData.accountName) {
                    LoginResponse(
                        success = false,
                        accountName = loginData.accountName,
                        error = "Token does not match requested account",
                    )
                } else {
                    LoginResponse(
                        success = true,
                        accountName = loginData.accountName,
                    )
                }
            }
            "static" -> {
                val success = properties.irc().fixedUsers()
                    .any { (user, password) ->
                        user.equals(loginData.accountName, ignoreCase = true) &&
                            password == authenticationValue
                    }

                LoginResponse(
                    success = success,
                    accountName = loginData.accountName,
                    error = if (success) {
                        null
                    } else {
                        "Username or password does not match for static user ${loginData.accountName}"
                    },
                )
            }
            else -> LoginResponse(
                success = false,
                accountName = loginData.accountName,
                error = "Unknown authentication typ $authenticationType",
            )
        }
    }
}
