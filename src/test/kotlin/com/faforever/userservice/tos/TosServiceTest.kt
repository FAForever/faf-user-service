package com.faforever.userservice.tos

import com.faforever.userservice.backend.domain.TermsOfService
import com.faforever.userservice.backend.domain.TermsOfServiceRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.tos.TosService
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@QuarkusTest
class TosServiceTest {
    companion object {
        private const val USER_ID = 1
        private const val LATEST_VERSION: Short = 2
        private val LATEST_TOS =
            TermsOfService(version = LATEST_VERSION, validFrom = LocalDateTime.now(), content = "Latest TOS")
    }

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var tosRepository: TermsOfServiceRepository

    @Inject
    private lateinit var tosService: TosService

    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        user = User(
            id = USER_ID,
            username = "testUser",
            password = "password",
            email = "test@example.com",
            ip = "127.0.0.1",
            acceptedTos = 1,
        )
        whenever(tosRepository.findLatest()).thenReturn(LATEST_TOS)
    }

    @Test
    fun testHasUserAcceptedLatestTosWhenAccepted() {
        user.acceptedTos = LATEST_VERSION

        whenever(userRepository.findById(eq(USER_ID))).thenReturn(user)
        val result = tosService.hasUserAcceptedLatestTos(USER_ID)

        assertTrue(result)
    }

    @Test
    fun testHasUserAcceptedLatestTosWhenNotAccepted() {
        whenever(userRepository.findById(eq(USER_ID))).thenReturn(user)
        val result = tosService.hasUserAcceptedLatestTos(USER_ID)

        assertFalse(result)
    }

    @Test
    fun testHasUserAcceptedLatestTosWhenTosNotExist() {
        whenever(tosRepository.findLatest()).thenReturn(null)
        whenever(userRepository.findById(eq(USER_ID))).thenReturn(user)
        val result = tosService.hasUserAcceptedLatestTos(USER_ID)

        assertFalse(result)
    }

    @Test
    fun testHasUserAcceptedLatestTosWhenUserNotExist() {
        whenever(userRepository.findById(eq(USER_ID))).thenReturn(null)

        assertThrows<IllegalStateException> { tosService.hasUserAcceptedLatestTos(USER_ID) }
    }

    @Test
    fun testAcceptLatestTosUpdatesUserAcceptedTos() {
        whenever(userRepository.findById(eq(USER_ID))).thenReturn(user)
        tosService.acceptLatestTos(USER_ID)

        verify(userRepository).persist(any<User>())
        assertEquals(LATEST_VERSION, user.acceptedTos)
    }

    @Test
    fun testAcceptLatestTosUserNotFoundThrowsException() {
        whenever(userRepository.findById(eq(USER_ID))).thenReturn(null)

        assertThrows<IllegalStateException> { tosService.acceptLatestTos(USER_ID) }
    }

    @Test
    fun testFindLatestTos() {
        assertNotNull(tosService.findLatestTos())
        verify(tosRepository).findLatest()
    }
}
