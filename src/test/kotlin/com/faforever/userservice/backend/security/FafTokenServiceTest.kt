package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.domain.AccountRequest
import com.faforever.userservice.backend.domain.AccountRequestRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration
import java.time.OffsetDateTime

@QuarkusTest
class FafTokenServiceTest {

    @Inject
    private lateinit var fafTokenService: FafTokenService

    @InjectMock
    private lateinit var accountRequestRepository: AccountRequestRepository

    @Test
    fun testTokenCreationAndParsing() {
        val token = fafTokenService.createToken(
            FafToken.Registration(username = "test", email = "test@test.com"),
            Duration.ofSeconds(60),
        )
        val decoded = fafTokenService.getToken(FafToken.Registration::class, token)
        assertThat(decoded.username, equalTo("test"))
        assertThat(decoded.email, equalTo("test@test.com"))
    }

    @Test
    fun testPasswordResetTokenCreationAndParsing() {
        val token = fafTokenService.createToken(
            FafToken.PasswordReset(userId = 42),
            Duration.ofSeconds(60),
        )
        val decoded = fafTokenService.getToken(FafToken.PasswordReset::class, token)
        assertThat(decoded.userId, equalTo(42))
    }

    @Test
    fun testEmailChangeTokenCreateAndConsume() {
        val requestCaptor = argumentCaptor<AccountRequest>()
        whenever(accountRequestRepository.findById(any())).thenAnswer {
            requestCaptor.firstValue
        }

        val token = fafTokenService.createToken(
            FafToken.EmailChange(userId = 7, newEmail = "new@example.com"),
            Duration.ofSeconds(60),
        )

        verify(accountRequestRepository).deleteByUserIdAndType(7, FafTokenType.EMAIL_CHANGE)
        verify(accountRequestRepository).persist(requestCaptor.capture())
        assertThat(requestCaptor.firstValue.id, equalTo(token))
        assertThat(requestCaptor.firstValue.userId, equalTo(7))
        assertThat(requestCaptor.firstValue.type, equalTo(FafTokenType.EMAIL_CHANGE))
        assertThat(requestCaptor.firstValue.data["newEmail"], equalTo("new@example.com"))
        assertThat(requestCaptor.firstValue.data["userId"], equalTo(7))

        val decoded = fafTokenService.consumeToken(FafToken.EmailChange::class, token)
        assertThat(decoded.userId, equalTo(7))
        assertThat(decoded.newEmail, equalTo("new@example.com"))
        verify(accountRequestRepository).delete(requestCaptor.firstValue)
    }

    @Test
    fun testEmailChangeConsumeRejectsExpired() {
        whenever(accountRequestRepository.findById("expired")).thenReturn(
            AccountRequest(
                id = "expired",
                userId = 1,
                type = FafTokenType.EMAIL_CHANGE,
                expiresAt = OffsetDateTime.now().minusMinutes(1),
                data = mapOf("userId" to 1, "newEmail" to "a@b.c"),
            ),
        )

        assertThrows<IllegalArgumentException> {
            fafTokenService.consumeToken(FafToken.EmailChange::class, "expired")
        }
    }

    @Test
    fun testTokenFailsWithWrongType() {
        val token = fafTokenService.createToken(
            FafToken.Registration(username = "test", email = "test@test.com"),
            Duration.ofSeconds(60),
        )
        assertThrows<IllegalArgumentException> {
            fafTokenService.getToken(FafToken.LinkToSteam::class, token)
        }
    }

    @Test
    fun testTokenFailsExpired() {
        val token = fafTokenService.createToken(
            FafToken.Registration(username = "test", email = "test@test.com"),
            Duration.ofSeconds(-60),
        )
        assertThrows<IllegalArgumentException> {
            fafTokenService.getToken(FafToken.Registration::class, token)
        }
    }

    @Test
    fun testConsumeRejectsJwtTokenTypes() {
        assertThrows<IllegalArgumentException> {
            fafTokenService.consumeToken(FafToken.Registration::class, "not-a-token")
        }
    }
}
