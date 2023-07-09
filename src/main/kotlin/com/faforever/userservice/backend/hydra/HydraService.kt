package com.faforever.userservice.backend.hydra

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.LoginResult
import com.faforever.userservice.backend.domain.LoginService
import com.faforever.userservice.backend.security.OAuthScope
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.eclipse.microprofile.rest.client.inject.RestClient
import sh.ory.hydra.model.AcceptLoginRequest
import sh.ory.hydra.model.GenericError
import java.net.URI

data class LoginResponseWithRedirect(
    val redirectTo: RedirectTo,
    val loginResult: LoginResult
)

@JvmInline
value class RedirectTo(val url: String) {
    val uri: URI get() = URI.create(url)
}

@Singleton
class HydraService(
    @RestClient
    private val hydraClient: HydraClient,
    private val loginService: LoginService,
) {
    companion object {
        private const val HYDRA_ERROR_USER_OR_CREDENTIALS_MISMATCH = "bad_credentials"
        private const val HYDRA_ERROR_USER_BANNED = "user_banned"
        private const val HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION = "ownership_not_verified"
        private const val HYDRA_ERROR_LOGIN_THROTTLED = "login_throttled"

    }

    @Transactional
    fun login(challenge: String, usernameOrEmail: String, password: String, ip: IpAddress): LoginResponseWithRedirect {
        val loginRequest = hydraClient.getLoginRequest(challenge)
        val requiresGameOwnership = loginRequest.requestedScope.contains(OAuthScope.LOBBY)

        val loginResult = loginService.login(usernameOrEmail, password, ip, requiresGameOwnership)

        val redirect = when(loginResult) {
            is LoginResult.LoginThrottlingActive -> hydraClient.rejectLoginRequest(challenge, GenericError(
                HYDRA_ERROR_LOGIN_THROTTLED
            ))
            is LoginResult.UserNoGameOwnership -> hydraClient.rejectLoginRequest(challenge, GenericError(
                HYDRA_ERROR_NO_OWNERSHIP_VERIFICATION
            ))
            is LoginResult.UserBanned -> {
                hydraClient.rejectLoginRequest(challenge, GenericError(HYDRA_ERROR_USER_BANNED))
            }
            is LoginResult.UserOrCredentialsMismatch -> hydraClient.rejectLoginRequest(challenge, GenericError(
                HYDRA_ERROR_USER_OR_CREDENTIALS_MISMATCH
            ))
            is LoginResult.SuccessfulLogin -> hydraClient.acceptLoginRequest(challenge, AcceptLoginRequest(
                subject = loginResult.userId.toString()
            ))
            is LoginResult.TechnicalError -> hydraClient.rejectLoginRequest(challenge, GenericError(
                HYDRA_ERROR_LOGIN_THROTTLED
            ))
        }

        return LoginResponseWithRedirect(
            RedirectTo(redirect.redirectTo),
            loginResult
        )
    }

}
