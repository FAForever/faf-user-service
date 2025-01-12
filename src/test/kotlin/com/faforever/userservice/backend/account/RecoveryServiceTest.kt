package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.account.RecoveryService.ParsingResult
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.metrics.MetricHelper
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.security.FafTokenType.PASSWORD_RESET
import com.faforever.userservice.backend.steam.SteamService
import com.faforever.userservice.config.FafProperties
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectSpy
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.Duration

@QuarkusTest
class RecoveryServiceTest {

    @Inject
    private lateinit var recoveryService: RecoveryService

    @Inject
    private lateinit var fafProperties: FafProperties

    @InjectSpy
    private lateinit var loginService: LoginService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var fafTokenService: FafTokenService

    @InjectSpy
    private lateinit var emailService: EmailService

    @InjectSpy
    private lateinit var metricHelper: MetricHelper

    @InjectSpy
    private lateinit var steamService: SteamService

    @InjectMock
    private lateinit var hydraService: HydraService

    @Test
    fun testBuildSteamLoginUrl() {
        // Execute
        val result = recoveryService.buildSteamLoginUrl()

        // Verify
        assertThat(result, startsWith("https://steamcommunity.com/openid/login"))
        assertThat(result, containsString("openid.ns"))
        assertThat(result, containsString("openid.mode"))
        assertThat(result, containsString("openid.return_to"))
        assertThat(result, containsString("openid.realm"))
        assertThat(result, containsString("openid.identity"))
        assertThat(result, containsString("openid.claimed_id"))
    }

    @Test
    fun testRequestPasswordResetViaEmailWithUnknownUser() {
        // Prepare
        whenever(userRepository.findByUsernameOrEmail("unknown user")).thenReturn(null)

        // Execute
        recoveryService.requestPasswordResetViaEmail("unknown user")

        // Verify
        verify(metricHelper).incrementPasswordResetViaEmailRequestCounter()
        verify(metricHelper).incrementPasswordResetViaEmailFailedCounter()

        verifyNoInteractions(fafTokenService)
        verifyNoInteractions(emailService)
    }

    @Test
    fun testRequestPasswordResetViaEmailWithKnownUser() {
        // Prepare
        val testUser = buildTestUser()
        whenever(userRepository.findByUsernameOrEmail(testUser.username))
            .thenReturn(testUser)

        // Execute
        recoveryService.requestPasswordResetViaEmail(testUser.username)

        // Verify
        verify(metricHelper).incrementPasswordResetViaEmailRequestCounter()
        verify(metricHelper).incrementPasswordResetViaEmailSentCounter()

        verify(fafTokenService).createToken(
            PASSWORD_RESET,
            Duration.ofSeconds(fafProperties.account().passwordReset().linkExpirationSeconds()),
            attributes = mapOf("id" to testUser.id.toString()),
        )
        verify(emailService).sendPasswordResetMail(eq(testUser.username), eq(testUser.email), any())
    }

    @Test
    fun testParseRecoveryHttpRequestWithEmptyParameters() {
        // Execute
        val result = recoveryService.parseRecoveryHttpRequest(emptyMap())

        // Verify
        assertThat(result, instanceOf(ParsingResult.Invalid::class.java))

        verify(metricHelper).incrementPasswordResetViaEmailFailedCounter()
    }

    @Test
    fun testParseRecoveryHttpRequestWithUnknownSteamId() {
        // Prepare
        val parameters = mapOf("token" to listOf("STEAM"))

        whenever(steamService.parseSteamIdFromRequestParameters(parameters))
            .thenReturn(SteamService.ParsingResult.ExtractedId("someSteamId"))
        whenever(steamService.findUserBySteamId("someSteamId"))
            .thenReturn(null)

        // Execute
        val result = recoveryService.parseRecoveryHttpRequest(parameters) as ParsingResult.ValidNoUser

        // Verify
        assertThat(result.type, equalTo(RecoveryService.Type.STEAM))
        verify(metricHelper).incrementPasswordResetViaSteamFailedCounter()
    }

    @Test
    fun testParseRecoveryHttpRequestWithKnownSteamId() {
        // Prepare
        val parameters = mapOf("token" to listOf("STEAM"))
        val testUser = buildTestUser()

        whenever(steamService.parseSteamIdFromRequestParameters(parameters))
            .thenReturn(SteamService.ParsingResult.ExtractedId("someSteamId"))
        whenever(steamService.findUserBySteamId("someSteamId"))
            .thenReturn(testUser)

        // Execute
        val result = recoveryService.parseRecoveryHttpRequest(parameters) as ParsingResult.ExtractedUser

        // Verify
        assertThat(result.type, equalTo(RecoveryService.Type.STEAM))
        assertThat(result.user, equalTo(testUser))
    }

    @Test
    fun testParseRecoveryHttpRequestWithInvalidTokenClaims() {
        // Prepare
        val parameters = mapOf("token" to listOf("tokenValue"))

        whenever(fafTokenService.getTokenClaims(PASSWORD_RESET, "tokenValue"))
            .thenThrow(RuntimeException("invalid token claim"))

        // Execute
        val result = recoveryService.parseRecoveryHttpRequest(parameters)

        // Verify
        assertThat(result, instanceOf(ParsingResult.Invalid::class.java))
        verify(metricHelper).incrementPasswordResetViaEmailFailedCounter()
    }

    @Test
    fun testParseRecoveryHttpRequestWithMissingUserIdInToken() {
        // Prepare
        val parameters = mapOf("token" to listOf("tokenValue"))

        whenever(steamService.parseSteamIdFromRequestParameters(parameters))
            .thenReturn(SteamService.ParsingResult.NoSteamIdPresent)
        whenever(fafTokenService.getTokenClaims(PASSWORD_RESET, "tokenValue"))
            .thenReturn(emptyMap())

        // Execute
        val result = recoveryService.parseRecoveryHttpRequest(parameters)

        // Verify
        assertThat(result, instanceOf(ParsingResult.Invalid::class.java))
        verify(metricHelper).incrementPasswordResetViaEmailFailedCounter()
    }

    @Test
    fun testParseRecoveryHttpRequestWithUnknownUserIdInToken() {
        // Prepare
        val parameters = mapOf("token" to listOf("tokenValue"))

        whenever(fafTokenService.getTokenClaims(PASSWORD_RESET, "tokenValue"))
            .thenReturn(mapOf("id" to "12345"))
        whenever(userRepository.findById(12345)).thenReturn(null)

        // Execute
        val result = recoveryService.parseRecoveryHttpRequest(parameters)

        // Verify
        assertThat(result, instanceOf(ParsingResult.Invalid::class.java))

        verify(metricHelper).incrementPasswordResetViaEmailFailedCounter()
    }

    @Test
    fun testParseRecoveryHttpRequestWithKnownUserIdInToken() {
        // Prepare
        val parameters = mapOf("token" to listOf("tokenValue"))
        val testUser = buildTestUser()

        whenever(steamService.parseSteamIdFromRequestParameters(parameters))
            .thenReturn(null)
        whenever(fafTokenService.getTokenClaims(PASSWORD_RESET, "tokenValue"))
            .thenReturn(mapOf("id" to "12345"))
        whenever(userRepository.findById(12345)).thenReturn(testUser)

        // Execute
        val result = recoveryService.parseRecoveryHttpRequest(parameters) as ParsingResult.ExtractedUser

        // Verify
        assertThat(result.type, equalTo(RecoveryService.Type.EMAIL))
        assertThat(result.user, equalTo(testUser))
    }

    @Test
    fun testResetPasswordEmail() {
        // Prepare
        val testUser = buildTestUser()
        whenever(userRepository.findById(testUser.id!!)).thenReturn(testUser)

        // Execute
        recoveryService.resetPassword(RecoveryService.Type.EMAIL, testUser.id!!, "banana")

        // Verify
        verify(loginService).resetPassword(testUser.id!!, "banana")
        verify(metricHelper).incrementPasswordResetViaEmailDoneCounter()
    }

    @Test
    fun testResetPasswordSteam() {
        // Prepare
        val testUser = buildTestUser()
        whenever(userRepository.findById(testUser.id!!)).thenReturn(testUser)

        // Execute
        recoveryService.resetPassword(RecoveryService.Type.STEAM, testUser.id!!, "banana")

        // Verify
        verify(loginService).resetPassword(testUser.id!!, "banana")
        verify(metricHelper).incrementPasswordResetViaSteamDoneCounter()
    }

    fun buildTestUser() =
        User(
            id = 1234,
            username = "testUser",
            password = "testPassword",
            email = "test@faforever.com",
            ip = null,
        )
}
