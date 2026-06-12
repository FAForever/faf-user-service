package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.AccountRequest
import com.faforever.userservice.backend.domain.AccountRequestRepository
import com.faforever.userservice.backend.domain.AccountRequestType
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.security.FafTokenType
import com.faforever.userservice.config.FafProperties
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.temporal.TemporalAmount
import java.util.Base64

@QuarkusTest
class EmailChangeServiceTest {

    @Inject
    private lateinit var emailChangeService: EmailChangeService

    @Inject
    private lateinit var fafProperties: FafProperties

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var accountRequestRepository: AccountRequestRepository

    @InjectMock
    private lateinit var emailService: EmailService

    @InjectMock
    private lateinit var fafTokenService: FafTokenService

    @Test
    fun requestEmailChangeCreatesPendingChangeAndSendsConfirmation() {
        val user = buildTestUser()
        val newEmail = "new@example.com"
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(emailService.validateEmailAddress(newEmail)).thenReturn(EmailService.ValidationResult.VALID)
        whenever(userRepository.findByEmail(newEmail)).thenReturn(null)
        whenever(
            fafTokenService.createToken(
                eq(FafTokenType.EMAIL_CHANGE),
                any<TemporalAmount>(),
                any(),
            ),
        ).thenReturn("token")

        val result = emailChangeService.requestEmailChange(user.id!!, newEmail)

        assertThat(result, equalTo(EmailChangeRequestResult.ConfirmationSent))
        verify(accountRequestRepository).deleteByUserIdAndType(user.id!!, AccountRequestType.EMAIL_CHANGE)
        argumentCaptor<AccountRequest>().apply {
            verify(accountRequestRepository).persist(capture())
            assertThat(firstValue.userId, equalTo(user.id))
            assertThat(firstValue.type, equalTo(AccountRequestType.EMAIL_CHANGE))
            assertThat(firstValue.tokenHash, equalTo(hashToken("token")))
            assertThat(firstValue.data["newEmail"], equalTo(newEmail))
        }
        verify(emailService).sendEmailChangeConfirmationMail(
            user.username,
            newEmail,
            fafProperties.account().emailChange().confirmationUrlFormat().format("token"),
        )
    }

    @Test
    fun requestEmailChangeRejectsTakenEmail() {
        val user = buildTestUser()
        val existingUser = buildTestUser(id = 2, username = "otherUser", email = "new@example.com")
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(emailService.validateEmailAddress(existingUser.email)).thenReturn(EmailService.ValidationResult.VALID)
        whenever(userRepository.findByEmail(existingUser.email)).thenReturn(existingUser)

        val result = emailChangeService.requestEmailChange(user.id!!, existingUser.email)

        assertThat(result, equalTo(EmailChangeRequestResult.EmailAlreadyTaken))
        verifyNoInteractions(accountRequestRepository)
    }

    @Test
    fun confirmEmailChangeUpdatesUserAndDeletesPendingChange() {
        val user = buildTestUser()
        val token = "token"
        val newEmail = "new@example.com"
        val pendingChange = AccountRequest(
            id = "change-id",
            userId = user.id!!,
            type = AccountRequestType.EMAIL_CHANGE,
            tokenHash = hashToken(token),
            expiresAt = OffsetDateTime.now().plusHours(1),
            data = mapOf("newEmail" to newEmail),
        )
        whenever(fafTokenService.getTokenClaims(FafTokenType.EMAIL_CHANGE, token)).thenReturn(
            mapOf(
                "changeId" to pendingChange.id,
                "userId" to user.id.toString(),
                "newEmail" to newEmail,
            ),
        )
        whenever(accountRequestRepository.findById(pendingChange.id)).thenReturn(pendingChange)
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(emailService.validateEmailAddress(newEmail)).thenReturn(EmailService.ValidationResult.VALID)
        whenever(userRepository.findByEmail(newEmail)).thenReturn(null)

        val result = emailChangeService.confirmEmailChange(token)

        assertThat(result, equalTo(EmailChangeConfirmationResult.Confirmed))
        assertThat(user.email, equalTo(newEmail))
        verify(accountRequestRepository).delete(pendingChange)
        verify(emailService).sendEmailChangedNotificationMail(user.username, "old@example.com", newEmail)
    }

    @Test
    fun requestEmailChangeTreatsCaseOnlyDifferenceAsUnchanged() {
        val user = buildTestUser(email = "old@example.com")
        val sameEmailDifferentCase = "Old@Example.com"
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(emailService.validateEmailAddress(sameEmailDifferentCase))
            .thenReturn(EmailService.ValidationResult.VALID)

        val result = emailChangeService.requestEmailChange(user.id!!, sameEmailDifferentCase)

        assertThat(result, equalTo(EmailChangeRequestResult.UnchangedEmail))
        verifyNoInteractions(accountRequestRepository)
    }

    @Test
    fun confirmEmailChangeRejectsInvalidToken() {
        val token = "token"
        whenever(fafTokenService.getTokenClaims(FafTokenType.EMAIL_CHANGE, token)).thenThrow(IllegalArgumentException())

        val result = emailChangeService.confirmEmailChange(token)

        assertThat(result, equalTo(EmailChangeConfirmationResult.InvalidToken))
        verifyNoInteractions(accountRequestRepository)
    }

    private fun buildTestUser(
        id: Int = 1,
        username: String = "testUser",
        email: String = "old@example.com",
    ) = User(
        id = id,
        username = username,
        password = "password",
        email = email,
        ip = null,
        acceptedTos = null,
    )

    private fun hashToken(token: String): String =
        Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.toByteArray()))
}
