package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.FriendOrFoe
import com.faforever.userservice.backend.domain.FriendOrFoeEntry
import com.faforever.userservice.backend.domain.FriendOrFoeRepository
import com.faforever.userservice.backend.domain.SocialStatus
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@QuarkusTest
class UcpFriendsFoesServiceTest {

    companion object {
        private const val USER_ID = 1
        private const val SUBJECT_ID = 2
        private const val SUBJECT_USERNAME = "Buddy"

        private val SUBJECT = User(
            id = SUBJECT_ID,
            username = SUBJECT_USERNAME,
            password = "password",
            email = "buddy@example.com",
            ip = null,
            acceptedTos = null,
        )
    }

    @Inject
    private lateinit var ucpFriendsFoesService: UcpFriendsFoesService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var friendOrFoeRepository: FriendOrFoeRepository

    @Test
    fun returnsFriendsFromRepository() {
        val entries = listOf(
            FriendOrFoeEntry(
                subjectId = SUBJECT_ID,
                username = SUBJECT_USERNAME,
                avatarUrl = "https://content.faforever.com/faf/avatars/UEF.png",
                avatarTooltip = "UEF",
            ),
        )
        whenever(friendOrFoeRepository.findEntriesByUserIdAndStatus(USER_ID, SocialStatus.FRIEND)).thenReturn(entries)

        assertEquals(entries, ucpFriendsFoesService.getFriends(USER_ID))
    }

    @Test
    fun returnsFoesFromRepository() {
        val entries = listOf(FriendOrFoeEntry(SUBJECT_ID, SUBJECT_USERNAME))
        whenever(friendOrFoeRepository.findEntriesByUserIdAndStatus(USER_ID, SocialStatus.FOE)).thenReturn(entries)

        assertEquals(entries, ucpFriendsFoesService.getFoes(USER_ID))
    }

    @Test
    fun addReturnsValidationErrorForBlankUsername() {
        val result = ucpFriendsFoesService.add(USER_ID, " ", SocialStatus.FRIEND)

        assertTrue(result is UcpFriendsFoesService.AddResult.ValidationError)
        assertEquals(
            "ucp.friendsFoes.error.empty",
            (result as UcpFriendsFoesService.AddResult.ValidationError).message,
        )
    }

    @Test
    fun addReturnsValidationErrorWhenSubjectNotFound() {
        whenever(userRepository.findByUsername(SUBJECT_USERNAME)).thenReturn(null)

        val result = ucpFriendsFoesService.add(USER_ID, SUBJECT_USERNAME, SocialStatus.FRIEND)

        assertTrue(result is UcpFriendsFoesService.AddResult.ValidationError)
        assertEquals(
            "ucp.friendsFoes.error.notFound",
            (result as UcpFriendsFoesService.AddResult.ValidationError).message,
        )
    }

    @Test
    fun addReturnsValidationErrorWhenAddingSelf() {
        whenever(userRepository.findByUsername(SUBJECT_USERNAME)).thenReturn(
            User(USER_ID, SUBJECT_USERNAME, "pw", "me@example.com", null, null),
        )

        val result = ucpFriendsFoesService.add(USER_ID, SUBJECT_USERNAME, SocialStatus.FRIEND)

        assertTrue(result is UcpFriendsFoesService.AddResult.ValidationError)
        assertEquals(
            "ucp.friendsFoes.error.self",
            (result as UcpFriendsFoesService.AddResult.ValidationError).message,
        )
    }

    @Test
    fun addReturnsValidationErrorWhenAlreadyOnSameList() {
        whenever(userRepository.findByUsername(SUBJECT_USERNAME)).thenReturn(SUBJECT)
        whenever(friendOrFoeRepository.findByUserIdAndSubjectId(USER_ID, SUBJECT_ID))
            .thenReturn(FriendOrFoe(USER_ID, SUBJECT_ID, SocialStatus.FRIEND))

        val result = ucpFriendsFoesService.add(USER_ID, SUBJECT_USERNAME, SocialStatus.FRIEND)

        assertTrue(result is UcpFriendsFoesService.AddResult.ValidationError)
        assertEquals(
            "ucp.friendsFoes.error.alreadyAdded",
            (result as UcpFriendsFoesService.AddResult.ValidationError).message,
        )
    }

    @Test
    fun addPersistsNewEntryWhenNoneExists() {
        whenever(userRepository.findByUsername(SUBJECT_USERNAME)).thenReturn(SUBJECT)
        whenever(friendOrFoeRepository.findByUserIdAndSubjectId(USER_ID, SUBJECT_ID)).thenReturn(null)

        val result = ucpFriendsFoesService.add(USER_ID, SUBJECT_USERNAME, SocialStatus.FRIEND)

        assertTrue(result is UcpFriendsFoesService.AddResult.Success)
        verify(friendOrFoeRepository).persist(FriendOrFoe(USER_ID, SUBJECT_ID, SocialStatus.FRIEND))
    }

    @Test
    fun addSwitchesStatusWhenAlreadyOnOppositeList() {
        whenever(userRepository.findByUsername(SUBJECT_USERNAME)).thenReturn(SUBJECT)
        val existing = FriendOrFoe(USER_ID, SUBJECT_ID, SocialStatus.FOE)
        whenever(friendOrFoeRepository.findByUserIdAndSubjectId(USER_ID, SUBJECT_ID)).thenReturn(existing)

        val result = ucpFriendsFoesService.add(USER_ID, SUBJECT_USERNAME, SocialStatus.FRIEND)

        assertTrue(result is UcpFriendsFoesService.AddResult.Success)
        assertEquals(SocialStatus.FRIEND, existing.status)
        verify(friendOrFoeRepository, never()).persist(FriendOrFoe(USER_ID, SUBJECT_ID, SocialStatus.FRIEND))
    }

    @Test
    fun removeReturnsSuccessWhenEntryDeleted() {
        whenever(friendOrFoeRepository.deleteByUserIdAndSubjectId(USER_ID, SUBJECT_ID)).thenReturn(true)

        assertEquals(UcpFriendsFoesService.RemoveResult.Success, ucpFriendsFoesService.remove(USER_ID, SUBJECT_ID))
    }

    @Test
    fun removeReturnsNotFoundWhenNoEntryDeleted() {
        whenever(friendOrFoeRepository.deleteByUserIdAndSubjectId(USER_ID, SUBJECT_ID)).thenReturn(false)

        assertEquals(UcpFriendsFoesService.RemoveResult.NotFound, ucpFriendsFoesService.remove(USER_ID, SUBJECT_ID))
    }
}
