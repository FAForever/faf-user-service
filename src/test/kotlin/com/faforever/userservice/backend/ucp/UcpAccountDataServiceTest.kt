package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Avatar
import com.faforever.userservice.backend.domain.AvatarAssignment
import com.faforever.userservice.backend.domain.AvatarAssignmentRepository
import com.faforever.userservice.backend.domain.AvatarRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

@QuarkusTest
class UcpAccountDataServiceTest {

    companion object {
        private const val USER_ID = 1

        private val USER = User(
            id = USER_ID,
            username = "Dostya",
            password = "vodka",
            email = "dostya@cybran.example.com",
            ip = null,
            acceptedTos = null,
        )

        private val AVATAR_ASSIGNMENT = AvatarAssignment(id = 1, idUser = USER_ID, idAvatar = 2, selected = true)

        private val AVATAR = Avatar(
            id = 2,
            tooltip = "UEF",
            createTime = null,
            updateTime = null,
            filename = "UEF.png",
            url = "https://content.faforever.com/faf/avatars/UEF.png",
            avatarTextDescription = null,
        )
    }

    @Inject
    private lateinit var ucpAccountDataService: UcpAccountDataService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var avatarAssignmentRepository: AvatarAssignmentRepository

    @InjectMock
    private lateinit var avatarRepository: AvatarRepository

    @Test
    fun throwsForUnknownUser() {
        assertThrows(IllegalArgumentException::class.java) {
            ucpAccountDataService.getAccountData(USER_ID)
        }
    }

    @Test
    fun returnsAccountDataWithoutAvatar() {
        whenever(userRepository.findById(USER_ID)).thenReturn(USER)

        val result = ucpAccountDataService.getAccountData(USER_ID)

        assertNotNull(result)
        assertEquals(USER.username, result.username)
        assertEquals(USER.email, result.email)
        assertNull(result.avatarUrl)
        assertNull(result.avatarTooltip)
    }

    @Test
    fun returnsAccountDataWithAvatar() {
        whenever(userRepository.findById(USER_ID)).thenReturn(USER)
        whenever(avatarAssignmentRepository.findSelectedAvatarByUserId(USER_ID)).thenReturn(AVATAR_ASSIGNMENT)
        whenever(avatarRepository.findById(AVATAR_ASSIGNMENT.idAvatar)).thenReturn(AVATAR)

        val result = ucpAccountDataService.getAccountData(USER_ID)

        assertEquals(AVATAR.url, result.avatarUrl)
        assertEquals(AVATAR.tooltip, result.avatarTooltip)
    }

    @Test
    fun returnsAccountDataWithoutAvatarWhenAssignmentExistsButAvatarMissing() {
        whenever(userRepository.findById(USER_ID)).thenReturn(USER)
        whenever(avatarAssignmentRepository.findSelectedAvatarByUserId(USER_ID)).thenReturn(AVATAR_ASSIGNMENT)
        whenever(avatarRepository.findById(AVATAR_ASSIGNMENT.idAvatar)).thenReturn(null)

        val result = ucpAccountDataService.getAccountData(USER_ID)

        assertEquals(USER.username, result.username)
        assertEquals(USER.email, result.email)
        assertNull(result.avatarUrl)
        assertNull(result.avatarTooltip)
    }

    @Test
    fun selectAvatarUpdatesSelectionFlags() {
        val assignment1 = AvatarAssignment(id = 1, idUser = USER_ID, idAvatar = 10, selected = false)
        val assignment2 = AvatarAssignment(id = 2, idUser = USER_ID, idAvatar = 20, selected = true)

        whenever(avatarAssignmentRepository.findAllByUserIdIncludingExpired(USER_ID))
            .thenReturn(listOf(assignment1, assignment2))
        whenever(avatarAssignmentRepository.findAssignmentByUserIdAndAvatarId(USER_ID, 10)).thenReturn(assignment1)

        val result = ucpAccountDataService.selectAvatar(USER_ID, 10)

        assertEquals(AvatarSelectionResult.Success, result)
        assertTrue(assignment1.selected)
        assertFalse(assignment2.selected)
    }

    @Test
    fun selectAvatarReturnsExpiredWhenAvatarAssignmentIsExpired() {
        val expiredAssignment = AvatarAssignment(
            id = 1,
            idUser = USER_ID,
            idAvatar = 10,
            selected = false,
            expiresAt = OffsetDateTime.now().minusMinutes(1),
        )

        whenever(avatarAssignmentRepository.findAllByUserIdIncludingExpired(USER_ID))
            .thenReturn(listOf(expiredAssignment))
        whenever(avatarAssignmentRepository.findAssignmentByUserIdAndAvatarId(USER_ID, 10)).thenReturn(null)

        val result = ucpAccountDataService.selectAvatar(USER_ID, 10)

        assertEquals(AvatarSelectionResult.AvatarExpired, result)
        assertFalse(expiredAssignment.selected)
    }

    @Test
    fun getAccountDataClearsExpiredSelectedAvatar() {
        whenever(userRepository.findById(USER_ID)).thenReturn(USER)
        val expiredAssignment = AvatarAssignment(
            id = 1,
            idUser = USER_ID,
            idAvatar = 2,
            selected = true,
            expiresAt = OffsetDateTime.now().minusMinutes(1),
        )

        whenever(avatarAssignmentRepository.findSelectedAvatarByUserId(USER_ID)).thenReturn(null)
        whenever(avatarAssignmentRepository.findExpiredSelectedAvatarByUserId(USER_ID)).thenReturn(expiredAssignment)

        val result = ucpAccountDataService.getAccountData(USER_ID)

        assertEquals(USER.username, result.username)
        assertNull(result.avatarUrl)
        assertNull(result.avatarTooltip)
        assertFalse(expiredAssignment.selected)
    }

    @Test
    fun deselectAvatarClearsAllSelections() {
        val a1 = AvatarAssignment(id = 1, idUser = USER_ID, idAvatar = 10, selected = true)
        val a2 = AvatarAssignment(id = 2, idUser = USER_ID, idAvatar = 11, selected = true)

        whenever(avatarAssignmentRepository.findAllByUserIdIncludingExpired(USER_ID))
            .thenReturn(listOf(a1, a2))

        ucpAccountDataService.deselectAvatar(USER_ID)

        assertFalse(a1.selected)
        assertFalse(a2.selected)
    }
}
