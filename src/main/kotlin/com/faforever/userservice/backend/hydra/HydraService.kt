package com.faforever.userservice.backend.hydra

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.LoginResult
import com.faforever.userservice.backend.domain.LoginService
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.security.OAuthScope
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.eclipse.microprofile.rest.client.inject.RestClient
import sh.ory.hydra.model.*
import java.net.URI

sealed interface LoginResponse {

    data class RejectedLogin(val redirectTo: RedirectTo) : LoginResponse
    data class FailedLogin(val userError: LoginResult.UserError) : LoginResponse
    data class SuccessfulLogin(val redirectTo: RedirectTo) : LoginResponse

}

@JvmInline
value class RedirectTo(val url: String) {
    val uri: URI get() = URI.create(url)
}

@Singleton
class HydraService(
    @RestClient private val hydraClient: HydraClient,
    private val loginService: LoginService,
    private val userRepository: UserRepository,
) {
    companion object {
        private const val HYDRA_ERROR_USER_BANNED = "user_banned"
        private const val HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION = "ownership_not_verified"
        private const val HYDRA_ERROR_TECHNICAL_ERROR = "technical_error"
    }

    fun getLoginRequest(challenge: String) : LoginRequest = hydraClient.getLoginRequest(challenge)

    @Transactional
    fun login(challenge: String, usernameOrEmail: String, password: String, ip: IpAddress): LoginResponse {
        val loginRequest = hydraClient.getLoginRequest(challenge)
        val requiresGameOwnership =
            loginRequest.requestedScope.contains(OAuthScope.LOBBY) || (loginRequest.requestedScope.isEmpty() && loginRequest.client.scope?.contains(
                OAuthScope.LOBBY
            ) ?: false)

        val loginResult = loginService.login(usernameOrEmail, password, ip, requiresGameOwnership)

        return when (loginResult) {
            is LoginResult.ThrottlingActive -> LoginResponse.FailedLogin(loginResult)
            is LoginResult.UserOrCredentialsMismatch -> LoginResponse.FailedLogin(loginResult)
            is LoginResult.UserNoGameOwnership -> {
                val redirectResponse = hydraClient.rejectLoginRequest(
                    challenge, GenericError(error = HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION, errorDescription = "You must prove game ownership to play", statusCode = 403)
                )
                LoginResponse.RejectedLogin(RedirectTo(redirectResponse.redirectTo))
            }
            is LoginResult.UserBanned -> {
                val redirectResponse =
                    hydraClient.rejectLoginRequest(challenge, GenericError(error = HYDRA_ERROR_USER_BANNED, errorDescription = "You are banned from FAF ${loginResult.expiresAt?.let { "until $it" } ?: "forever"}", statusCode = 403))
                LoginResponse.RejectedLogin(RedirectTo(redirectResponse.redirectTo))
            }
            is LoginResult.TechnicalError -> {
                val redirectResponse = hydraClient.rejectLoginRequest(
                    challenge, GenericError(error = HYDRA_ERROR_TECHNICAL_ERROR, errorDescription = "Something went wrong while logging in. Please try again", statusCode = 500)
                )
                LoginResponse.RejectedLogin(RedirectTo(redirectResponse.redirectTo))
            }
            is LoginResult.SuccessfulLogin -> {
                val redirectResponse = hydraClient.acceptLoginRequest(
                    challenge, AcceptLoginRequest(subject = loginResult.userId.toString())
                )
                LoginResponse.SuccessfulLogin(RedirectTo(redirectResponse.redirectTo))
            }
        }
    }

    fun getConsentRequest(challenge: String) : ConsentRequest = hydraClient.getConsentRequest(challenge)

    @Transactional
    fun acceptConsentRequest(challenge: String) : RedirectTo {
        val consentRequest = hydraClient.getConsentRequest(challenge)

        val userId = consentRequest.subject?.toInt() ?: -1
        val user = userRepository.findById(userId) ?: throw IllegalStateException("Unknown user with id $userId")

        val permissions = userRepository.findUserPermissions(userId)

        val roles = listOf("USER") + permissions.map { it.technicalName }

        val redirectResponse = hydraClient.acceptConsentRequest(
            challenge, AcceptConsentRequest(
                session = ConsentRequestSession(
                    accessToken = mapOf("username" to user.username, "roles" to roles),
                    idToken = mapOf("username" to user.username, "roles" to roles),
                ),
                grantScope = consentRequest.requestedScope,
            )
        )

        return RedirectTo(redirectResponse.redirectTo)
    }

    fun denyConsentRequest(challenge: String) : RedirectTo {
        val redirectResponse = hydraClient.rejectConsentRequest(challenge, GenericError("scope_denied"))
        return RedirectTo(redirectResponse.redirectTo)
    }

}
