package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Group
import com.faforever.userservice.backend.domain.UserRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever

@QuarkusTest
class UcpGroupsServiceTest {

    companion object {
        private const val USER_ID = 1

        private val GROUP = Group(
            id = 28,
            technicalName = "user_group.testgroup2",
        )
    }

    @Inject
    private lateinit var ucpGroupsService: UcpGroupsService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @Test
    fun returnsGroupsFromRepository() {
        whenever(userRepository.findUserGroups(USER_ID)).thenReturn(listOf(GROUP))

        val result = ucpGroupsService.getGroupsForUser(USER_ID)

        assertEquals(1, result.size)
        assertEquals(GROUP.technicalName, result[0].technicalName)
    }

    @Test
    fun returnsEmptyListWhenNoGroups() {
        whenever(userRepository.findUserGroups(USER_ID)).thenReturn(emptyList())

        val result = ucpGroupsService.getGroupsForUser(USER_ID)

        assertTrue(result.isEmpty())
    }
}
