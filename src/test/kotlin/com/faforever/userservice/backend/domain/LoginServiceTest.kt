package com.faforever.userservice.backend.domain

import com.faforever.userservice.backend.account.LoginResult
import com.faforever.userservice.backend.account.LoginServiceImpl
import com.faforever.userservice.backend.account.SecurityProperties
import com.faforever.userservice.backend.security.PasswordEncoder
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.OffsetDateTime

@QuarkusTest
class LoginServiceTest {
    companion object {
        private const val USERNAME = "someUsername"
        private const val EMAIL = "some@email.com"
        private const val PASSWORD = "somePassword"
        private val IP_ADDRESS = IpAddress("127.0.0.1")

        private val USER = User(1, USERNAME, PASSWORD, EMAIL, null, null)
    }

    @Inject
    private lateinit var loginService: LoginServiceImpl

    @Inject
    private lateinit var securityProperties: SecurityProperties

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var loginLogRepository: LoginLogRepository

    @InjectMock
    private lateinit var accountLinkRepository: AccountLinkRepository

    @InjectMock
    private lateinit var passwordEncoder: PasswordEncoder

    @InjectMock
    private lateinit var banRepository: BanRepository

    @Test
    fun loginWithUnknownUser() {
        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.RecoverableLoginOrCredentialsMismatch::class.java))
    }

    @Test
    fun loginWithThrottling() {
        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(any(), any())).thenReturn(
            FailedAttemptsSummary(
                100,
                1,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().minusSeconds(10),
            ),
        )

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.ThrottlingActive::class.java))
    }

    @Test
    fun loginWithInvalidPassword() {
        whenever(userRepository.findByUsernameOrEmail(any())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(false)

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.RecoverableLoginOrCredentialsMismatch::class.java))
    }

    @Test
    fun loginWithBannedUser() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
        whenever(banRepository.findGlobalBansByPlayerId(anyInt())).thenReturn(
            listOf(
                Ban(1, 1, 100, BanLevel.GLOBAL, "test", OffsetDateTime.MAX, null, null, null, null),
            ),
        )

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.UserBanned::class.java))
    }

    @Test
    fun loginWithPermaBannedUser() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
        whenever(banRepository.findGlobalBansByPlayerId(anyInt())).thenReturn(
            listOf(
                Ban(1, 1, 100, BanLevel.GLOBAL, "test", null, null, null, null, null),
            ),
        )

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.UserBanned::class.java))
    }

    @Test
    fun loginWithLinkedUserRequireOwnership() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
        whenever(accountLinkRepository.hasOwnershipLink(anyInt())).thenReturn(true)

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, true)
        assertThat(result, instanceOf(LoginResult.SuccessfulLogin::class.java))
    }

    @Test
    fun loginWithNonLinkedUserRequireOwnership() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, true)
        assertThat(result, instanceOf(LoginResult.UserNoGameOwnership::class.java))
    }

    @Test
    fun loginWithNonLinkedUser() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.SuccessfulLogin::class.java))
    }

    @Test
    fun loginWithUnbannedUser() {
        whenever(banRepository.findGlobalBansByPlayerId(anyInt())).thenReturn(
            listOf(
                Ban(
                    1,
                    1,
                    100,
                    BanLevel.GLOBAL,
                    "test",
                    OffsetDateTime.MAX,
                    OffsetDateTime.now().minusDays(1),
                    null,
                    null,
                    null,
                ),
            ),
        )
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.SuccessfulLogin::class.java))
    }

    @Test
    fun loginWithPreviouslyBannedUser() {
        whenever(banRepository.findGlobalBansByPlayerId(anyInt())).thenReturn(
            listOf(
                Ban(
                    1,
                    1,
                    100,
                    BanLevel.GLOBAL,
                    "test",
                    OffsetDateTime.MIN,
                    OffsetDateTime.now().minusDays(1),
                    null,
                    null,
                    null,
                ),
            ),
        )
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(USER)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(USERNAME, PASSWORD, IP_ADDRESS, false)
        assertThat(result, instanceOf(LoginResult.SuccessfulLogin::class.java))
    }
}
