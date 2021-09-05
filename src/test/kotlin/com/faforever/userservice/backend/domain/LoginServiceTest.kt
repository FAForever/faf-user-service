package com.faforever.userservice.backend.domain

import com.faforever.domain.Ban
import com.faforever.domain.BanLevel
import com.faforever.domain.BanRepository
import com.faforever.userservice.backend.login.LoginResult
import com.faforever.userservice.backend.login.LoginServiceImpl
import com.faforever.userservice.backend.login.SecurityProperties
import com.faforever.userservice.backend.security.PasswordEncoder
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectMock
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
        private const val username = "someUsername"
        private const val email = "some@email.com"
        private const val password = "somePassword"
        private val ipAddress = IpAddress("127.0.0.1")

        private val user = User(1, username, password, email, null)
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
        val result = loginService.login(username, password, ipAddress, false)
        assertThat(result, instanceOf(LoginResult.UserOrCredentialsMismatch::class.java))
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

        val result = loginService.login(username, password, ipAddress, false)
        assertThat(result, instanceOf(LoginResult.ThrottlingActive::class.java))
    }

    @Test
    fun loginWithInvalidPassword() {
        whenever(userRepository.findByUsernameOrEmail(any())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(false)

        val result = loginService.login(username, password, ipAddress, false)
        assertThat(result, instanceOf(LoginResult.UserOrCredentialsMismatch::class.java))
    }

    @Test
    fun loginWithBannedUser() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
        whenever(banRepository.findGlobalBansByPlayerId(anyInt())).thenReturn(
            listOf(
                Ban(1, 1, 100, BanLevel.GLOBAL, "test", OffsetDateTime.MAX, null, null, null, null)
            )
        )

        val result = loginService.login(username, password, ipAddress, false)
        assertThat(result, instanceOf(LoginResult.UserBanned::class.java))
    }

    @Test
    fun loginWithPermaBannedUser() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
        whenever(banRepository.findGlobalBansByPlayerId(anyInt())).thenReturn(
            listOf(
                Ban(1, 1, 100, BanLevel.GLOBAL, "test", null, null, null, null, null)
            )
        )

        val result = loginService.login(username, password, ipAddress, false)
        assertThat(result, instanceOf(LoginResult.UserBanned::class.java))
    }

    @Test
    fun loginWithLinkedUserRequireOwnership() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)
        whenever(accountLinkRepository.hasOwnershipLink(anyInt())).thenReturn(true)

        val result = loginService.login(username, password, ipAddress, true)
        assertThat(result, instanceOf(LoginResult.SuccessfulLogin::class.java))
    }

    @Test
    fun loginWithNonLinkedUserRequireOwnership() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(username, password, ipAddress, true)
        assertThat(result, instanceOf(LoginResult.UserNoGameOwnership::class.java))
    }

    @Test
    fun loginWithNonLinkedUser() {
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(username, password, ipAddress, false)
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
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(username, password, ipAddress, false)
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
        whenever(userRepository.findByUsernameOrEmail(anyString())).thenReturn(user)
        whenever(passwordEncoder.matches(anyString(), anyString())).thenReturn(true)

        val result = loginService.login(username, password, ipAddress, false)
        assertThat(result, instanceOf(LoginResult.SuccessfulLogin::class.java))
    }


}