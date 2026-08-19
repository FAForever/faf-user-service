package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.PasswordEncoder
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@QuarkusTest
class PasswordChangeServiceTest {

    @Inject
    private lateinit var passwordChangeService: PasswordChangeService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var emailService: EmailService

    @InjectMock
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun changePasswordSuccess() {
        val user = buildTestUser()
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(passwordEncoder.matches("currentPassword", user.password)).thenReturn(true)
        whenever(passwordEncoder.matches("newPassword123", user.password)).thenReturn(false)
        whenever(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword")

        val result = passwordChangeService.changePassword(user.id!!, "currentPassword", "newPassword123")

        assertThat(result, equalTo(PasswordChangeResult.Success))
        assertThat(user.password, equalTo("encodedNewPassword"))
        verify(userRepository).persist(user)
        verify(emailService).sendPasswordChangedNotificationMail(user.username, user.email)
    }

    @Test
    fun changePasswordRejectsIncorrectCurrentPassword() {
        val user = buildTestUser()
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(passwordEncoder.matches("wrongPassword", user.password)).thenReturn(false)

        val result = passwordChangeService.changePassword(user.id!!, "wrongPassword", "newPassword123")

        assertThat(result, equalTo(PasswordChangeResult.InvalidCurrentPassword))
        verifyNoInteractions(emailService)
    }

    @Test
    fun changePasswordRejectsSamePassword() {
        val user = buildTestUser()
        whenever(userRepository.findById(user.id!!)).thenReturn(user)
        whenever(passwordEncoder.matches("currentPassword", user.password)).thenReturn(true)

        val result = passwordChangeService.changePassword(user.id!!, "currentPassword", "currentPassword")

        assertThat(result, equalTo(PasswordChangeResult.PasswordUnchanged))
        verifyNoInteractions(emailService)
    }

    @Test
    fun changePasswordThrowsWhenUserNotFound() {
        whenever(userRepository.findById(999)).thenReturn(null)

        assertThrows<IllegalArgumentException> {
            passwordChangeService.changePassword(999, "currentPassword", "newPassword123")
        }
        verifyNoInteractions(emailService)
    }

    private fun buildTestUser(
        id: Int = 1,
        username: String = "testUser",
        email: String = "test@example.com",
        password: String = "currentPassword",
    ) = User(
        id = id,
        username = username,
        password = password,
        email = email,
        ip = null,
        acceptedTos = null,
    )
}
