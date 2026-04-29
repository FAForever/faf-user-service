package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Permission
import com.faforever.userservice.backend.domain.UserRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@QuarkusTest
class UcpPermissionServiceTest {

    companion object {
        private const val USER_ID = 1

        private val PERMISSION = Permission(
            id = 6,
            technicalName = "ADMIN_ACCOUNT_BAN",
            createTime = LocalDateTime.now(),
            updateTime = LocalDateTime.now(),
        )
    }

    @Inject
    private lateinit var ucpPermissionService: UcpPermissionService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @Test
    fun returnsPermissionsFromRepository() {
        whenever(userRepository.findUserPermissions(USER_ID)).thenReturn(listOf(PERMISSION))

        val result = ucpPermissionService.getPermissionsForUser(USER_ID)

        assertEquals(1, result.size)
        assertEquals(PERMISSION.technicalName, result[0].technicalName)
    }

    @Test
    fun returnsEmptyListWhenNoPermissions() {
        whenever(userRepository.findUserPermissions(USER_ID)).thenReturn(emptyList())

        val result = ucpPermissionService.getPermissionsForUser(USER_ID)

        assertTrue(result.isEmpty())
    }
}
