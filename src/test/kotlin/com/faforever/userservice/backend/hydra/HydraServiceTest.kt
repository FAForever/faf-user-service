package com.faforever.userservice.backend.hydra

import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.LoginResult
import com.faforever.userservice.backend.domain.LoginService
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.security.OAuthScope
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import sh.ory.hydra.model.LoginRequest
import sh.ory.hydra.model.OAuth2Client
import java.net.http.HttpClient
import java.net.http.HttpResponse.BodyHandler
import java.time.OffsetDateTime

@QuarkusTest
class HydraServiceTest {

    companion object {
        val ipAddress = IpAddress("127.0.0.1")
        val loginRequest = LoginRequest("", OAuth2Client(), "", listOf(), listOf(), false, "1")
        val lobbyLoginRequest = LoginRequest("", OAuth2Client(), "", listOf(), listOf(OAuthScope.LOBBY), false, "1")
        val implicitLobbyLoginRequest =
            LoginRequest("", OAuth2Client(scope = OAuthScope.LOBBY), "", listOf(), listOf(), false, "1")
        val noLobbyLoginRequest =
            LoginRequest("", OAuth2Client(scope = OAuthScope.LOBBY), "", listOf(), listOf("test"), false, "1")
    }

    @Inject
    private lateinit var hydraService: HydraService

    @InjectMock
    private lateinit var httpClient: HttpClient

    @InjectMock
    @RestClient
    private lateinit var hydraClient: HydraClient

    @InjectMock
    private lateinit var loginService: LoginService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @Test
    fun testLoginThrottling() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(loginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.ThrottlingActive,
        )

        val response = hydraService.login("test", "", "", ipAddress)

        assertThat(response, instanceOf(LoginResponse.FailedLogin::class.java))
        assertThat(
            (response as LoginResponse.FailedLogin).recoverableLoginFailure,
            instanceOf(LoginResult.ThrottlingActive::class.java),
        )
    }

    @Test
    fun testCredentialsMismatch() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(loginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.RecoverableLoginOrCredentialsMismatch,
        )

        val response = hydraService.login("test", "", "", ipAddress)

        assertThat(response, instanceOf(LoginResponse.FailedLogin::class.java))
        assertThat(
            (response as LoginResponse.FailedLogin).recoverableLoginFailure,
            instanceOf(LoginResult.RecoverableLoginOrCredentialsMismatch::class.java),
        )
    }

    @Test
    fun testNoOwnership() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(lobbyLoginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.UserNoGameOwnership,
        )
        whenever(hydraClient.rejectLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        val response = hydraService.login("test", "", "", ipAddress)

        assertThat(response, instanceOf(LoginResponse.RejectedLogin::class.java))
        assertThat(
            (response as LoginResponse.RejectedLogin).unrecoverableLoginFailure,
            instanceOf(LoginResult.UserNoGameOwnership::class.java),
        )
        verify(hydraClient).rejectLoginRequest(anyString(), any())
        verify(httpClient).sendAsync(any(), any<BodyHandler<Void>>())
    }

    @Test
    fun testUserBanned() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(loginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.UserBanned("", OffsetDateTime.MAX),
        )
        whenever(hydraClient.rejectLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        val response = hydraService.login("test", "", "", ipAddress)

        assertThat(response, instanceOf(LoginResponse.RejectedLogin::class.java))
        assertThat(
            (response as LoginResponse.RejectedLogin).unrecoverableLoginFailure,
            instanceOf(LoginResult.UserBanned::class.java),
        )
        verify(hydraClient).rejectLoginRequest(anyString(), any())
        verify(httpClient).sendAsync(any(), any<BodyHandler<Void>>())
    }

    @Test
    fun testTechnicalError() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(loginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(LoginResult.TechnicalError)
        whenever(hydraClient.rejectLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        val response = hydraService.login("test", "", "", ipAddress)

        assertThat(response, instanceOf(LoginResponse.RejectedLogin::class.java))
        assertThat(
            (response as LoginResponse.RejectedLogin).unrecoverableLoginFailure,
            instanceOf(LoginResult.TechnicalError::class.java),
        )
        verify(hydraClient).rejectLoginRequest(anyString(), any())
        verify(httpClient).sendAsync(any(), any<BodyHandler<Void>>())
    }

    @Test
    fun testSuccess() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(loginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.SuccessfulLogin(1, "test"),
        )
        whenever(hydraClient.acceptLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        val response = hydraService.login("test", "", "", ipAddress)

        assertThat(response, instanceOf(LoginResponse.SuccessfulLogin::class.java))
    }

    @Test
    fun testOwnershipRequiredExplicitRequest() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(lobbyLoginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.SuccessfulLogin(1, ""),
        )
        whenever(hydraClient.acceptLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        hydraService.login("test", "", "", ipAddress)

        verify(loginService).login(any(), any(), IpAddress(anyString()), eq(true))
    }

    @Test
    fun testOwnershipRequiredImplicitRequest() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(implicitLobbyLoginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.SuccessfulLogin(1, ""),
        )
        whenever(hydraClient.acceptLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        hydraService.login("test", "", "", ipAddress)

        verify(loginService).login(any(), any(), IpAddress(anyString()), eq(true))
    }

    @Test
    fun testOwnershipNotRequested() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(noLobbyLoginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.SuccessfulLogin(1, ""),
        )
        whenever(hydraClient.acceptLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        hydraService.login("test", "", "", ipAddress)

        verify(loginService).login(any(), any(), IpAddress(anyString()), eq(false))
    }

    @Test
    fun testOwnershipNotApplicable() {
        whenever(hydraClient.getLoginRequest(any())).thenReturn(noLobbyLoginRequest)
        whenever(loginService.login(any(), any(), IpAddress(anyString()), any())).thenReturn(
            LoginResult.SuccessfulLogin(1, ""),
        )
        whenever(hydraClient.acceptLoginRequest(anyString(), any())).thenReturn(RedirectResponse("http://localhost"))

        hydraService.login("test", "", "", ipAddress)

        verify(loginService).login(any(), any(), IpAddress(anyString()), eq(false))
    }
}
