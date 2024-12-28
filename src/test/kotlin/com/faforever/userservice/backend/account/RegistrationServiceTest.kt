package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.DomainBlacklistRepository
import com.faforever.userservice.backend.domain.IpAddress
import com.faforever.userservice.backend.domain.NameRecordRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.config.FafProperties
import io.quarkus.mailer.MockMailbox
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectSpy
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@QuarkusTest
class RegistrationServiceTest {

    companion object {
        private const val username = "someUsername"
        private const val email = "some@email.com"
        private const val password = "somePassword"
        private val ipAddress = IpAddress("127.0.0.1")

        private val user = User(1, username, password, email, null, null)
    }

    @InjectSpy
    private lateinit var emailService: EmailService

    @Inject
    private lateinit var registrationService: RegistrationService

    @Inject
    private lateinit var mailbox: MockMailbox

    @Inject
    private lateinit var fafProperties: FafProperties

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var nameRecordRepository: NameRecordRepository

    @InjectMock
    private lateinit var domainBlacklistRepository: DomainBlacklistRepository

    @InjectMock
    private lateinit var fafTokenService: FafTokenService

    @BeforeEach
    fun setup() {
        mailbox.clear()
    }

    @Test
    fun registerSuccess() {
        registrationService.register(username, email)

        val sent = mailbox.getMailsSentTo(email)
        assertThat(sent, hasSize(1))
        val actual = sent[0]
        assertThat(actual.subject, `is`(fafProperties.account().registration().subject()))
    }

    @Test
    fun registerUsernameTaken() {
        whenever(userRepository.existsByUsername(anyString())).thenReturn(true)

        assertThrows<IllegalArgumentException> { registrationService.register(username, email) }
    }

    @Test
    fun registerUsernameReserved() {
        whenever(nameRecordRepository.existsByPreviousNameAndChangeTimeAfter(anyString(), any())).thenReturn(true)

        assertThrows<IllegalArgumentException> { registrationService.register(username, email) }
    }

    @Test
    fun registerEmailTaken() {
        whenever(userRepository.findByEmail(anyString())).thenReturn(user)
        val newTestUsername = "newUsername"

        registrationService.register(newTestUsername, email)

        val expectedLink = fafProperties.account().passwordReset().passwordResetInitiateEmailUrlFormat().format(email)
        verify(emailService, times(1)).sendEmailAlreadyTakenMail(newTestUsername, username, email, expectedLink)
    }

    @Test
    fun registerEmailBlacklisted() {
        whenever(domainBlacklistRepository.existsByDomain(anyString())).thenReturn(true)

        assertThrows<IllegalStateException> { registrationService.register(username, email) }
    }

    @Test
    fun activateSuccess() {
        registrationService.activate(RegisteredUser(username, email), ipAddress, password)

        verify(userRepository).persist(any<User>())

        val sent = mailbox.getMailsSentTo(email)
        assertThat(sent, hasSize(1))
        val actual = sent[0]
        assertEquals(fafProperties.account().registration().welcomeSubject(), actual.subject)
    }

    @Test
    fun activateUsernameTaken() {
        whenever(userRepository.existsByUsername(anyString())).thenReturn(true)

        assertThrows<IllegalArgumentException> {
            registrationService.activate(RegisteredUser(username, email), ipAddress, password)
        }
    }

    @Test
    fun activateUsernameReserved() {
        whenever(nameRecordRepository.existsByPreviousNameAndChangeTimeAfter(anyString(), any())).thenReturn(true)

        assertThrows<IllegalArgumentException> {
            registrationService.activate(RegisteredUser(username, email), ipAddress, password)
        }
    }

    @Test
    fun activateEmailTaken() {
        whenever(userRepository.findByEmail(anyString())).thenReturn(user)

        assertThrows<IllegalArgumentException> {
            registrationService.activate(RegisteredUser(username, email), ipAddress, password)
        }
    }

    @Test
    fun activateEmailBlacklisted() {
        whenever(domainBlacklistRepository.existsByDomain(anyString())).thenReturn(true)

        assertThrows<IllegalArgumentException> {
            registrationService.activate(RegisteredUser(username, email), ipAddress, password)
        }
    }
}
