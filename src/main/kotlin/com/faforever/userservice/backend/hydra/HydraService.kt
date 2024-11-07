package com.faforever.userservice.backend.hydra

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.login.LoginResult
import com.faforever.userservice.backend.login.LoginService
import com.faforever.userservice.backend.security.OAuthScope
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.transaction.Transactional
import org.eclipse.microprofile.rest.client.inject.RestClient
import sh.ory.hydra.model.AcceptConsentRequest
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.ConsentRequest
import sh.ory.hydra.model.ConsentRequestSession
import sh.ory.hydra.model.GenericError
import sh.ory.hydra.model.LoginRequest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

sealed interface LoginResponse {

    data class RejectedLogin(val unrecoverableLoginFailure: LoginResult.UnrecoverableLoginFailure) : LoginResponse
    data class FailedLogin(val recoverableLoginFailure: LoginResult.RecoverableLoginFailure) : LoginResponse
    data class SuccessfulLogin(val redirectTo: RedirectTo) : LoginResponse
}

@JvmInline
value class RedirectTo(private val url: String) {
    val uri: URI get() = URI.create(url)
}

object HttpClientProducer {
    @Produces
    @ApplicationScoped
    fun httpClient(): HttpClient {
        return HttpClient.newHttpClient()
    }
}

@ApplicationScoped
class HydraService(
    @RestClient private val hydraClient: HydraClient,
    private val httpClient: HttpClient,
    private val loginService: LoginService,
    private val userRepository: UserRepository,
) {
    companion object {
        private const val HYDRA_ERROR_USER_BANNED = "user_banned"
        private const val HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION = "ownership_not_verified"
        private const val HYDRA_ERROR_TECHNICAL_ERROR = "technical_error"
    }

    fun getLoginRequest(challenge: String): LoginRequest = hydraClient.getLoginRequest(challenge)

    @Transactional
    fun login(challenge: String, usernameOrEmail: String, password: String, ip: IpAddress): LoginResponse {
        val loginRequest = hydraClient.getLoginRequest(challenge)
        val lobbyRequested = loginRequest.requestedScope.contains(OAuthScope.LOBBY)
        val lobbyDefault =
            loginRequest.requestedScope.isEmpty() && loginRequest.client.scope?.contains(OAuthScope.LOBBY) ?: false
        val requiresGameOwnership = lobbyRequested || lobbyDefault

        return when (val loginResult = loginService.login(usernameOrEmail, password, ip, requiresGameOwnership)) {
            is LoginResult.ThrottlingActive -> LoginResponse.FailedLogin(loginResult)
            is LoginResult.RecoverableLoginOrCredentialsMismatch -> LoginResponse.FailedLogin(loginResult)
            is LoginResult.UserNoGameOwnership -> {
                rejectLoginRequest(
                    challenge,
                    GenericError(
                        error = HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION,
                        errorDescription = "You must prove game ownership to play",
                        statusCode = 403,
                    ),
                )
                LoginResponse.RejectedLogin(loginResult)
            }

            is LoginResult.UserBanned -> {
                val errorDescription =
                    "You are banned from FAF ${loginResult.expiresAt?.let { "until $it" } ?: "forever"}"
                rejectLoginRequest(
                    challenge,
                    GenericError(
                        error = HYDRA_ERROR_USER_BANNED,
                        errorDescription = errorDescription,
                        statusCode = 403,
                    ),
                )
                LoginResponse.RejectedLogin(loginResult)
            }

            is LoginResult.TechnicalError -> {
                rejectLoginRequest(
                    challenge,
                    GenericError(
                        error = HYDRA_ERROR_TECHNICAL_ERROR,
                        errorDescription = "Something went wrong while logging in. Please try again",
                        statusCode = 500,
                    ),
                )
                LoginResponse.RejectedLogin(loginResult)
            }

            is LoginResult.SuccessfulLogin -> {
                val redirectResponse = hydraClient.acceptLoginRequest(
                    challenge,
                    AcceptLoginRequest(subject = loginResult.userId.toString()),
                )
                LoginResponse.SuccessfulLogin(RedirectTo(redirectResponse.redirectTo))
            }
        }
    }

    fun rejectLoginRequest(challenge: String, error: GenericError) {
        val redirectResponse = hydraClient.rejectLoginRequest(
            challenge,
            GenericError(
                error = HYDRA_ERROR_TECHNICAL_ERROR,
                errorDescription = "Something went wrong while logging in. Please try again",
                statusCode = 500,
            ),
        )
        httpClient.sendAsync(
            HttpRequest.newBuilder(URI.create(redirectResponse.redirectTo)).build(),
            BodyHandlers.discarding(),
        )
    }

    fun getConsentRequest(challenge: String): ConsentRequest = hydraClient.getConsentRequest(challenge)

    @Transactional
    fun acceptConsentRequest(challenge: String): RedirectTo {
        val consentRequest = hydraClient.getConsentRequest(challenge)

        val userId = consentRequest.subject?.toInt() ?: -1
        val user = userRepository.findById(userId) ?: throw IllegalStateException("Unknown user with id $userId")

        val permissions = userRepository.findUserPermissions(userId)

        val roles = listOf("USER") + permissions.map { it.technicalName }

        val context = mutableMapOf(
            "username" to user.username, // not official OIDC claim, but required for backwards compatible
            "preferred_username" to user.username,
            "roles" to roles,
        )

        if (OAuthScope.canShowEmail(consentRequest.requestedScope)) {
            context["email"] = user.email
            context["email_verified"] = true
        }

        val redirectResponse = hydraClient.acceptConsentRequest(
            challenge,
            AcceptConsentRequest(
                session = ConsentRequestSession(
                    accessToken = context,
                    idToken = context,
                ),
                grantScope = consentRequest.requestedScope,
            ),
        )

        return RedirectTo(redirectResponse.redirectTo)
    }

    fun denyConsentRequest(challenge: String): RedirectTo {
        val redirectResponse = hydraClient.rejectConsentRequest(challenge, GenericError("scope_denied"))
        return RedirectTo(redirectResponse.redirectTo)
    }
}
