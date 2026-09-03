package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.AccountLinkRepository
import com.faforever.userservice.backend.domain.BanRepository
import com.faforever.userservice.backend.domain.GamePlayerStatsRepository
import com.faforever.userservice.backend.domain.LeaderboardRatingRepository
import com.faforever.userservice.backend.domain.LoginLogRepository
import com.faforever.userservice.backend.domain.NameRecordRepository
import com.faforever.userservice.backend.domain.UniqueIdUserRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AccountAnonymizationServiceTest {

    private companion object {
        private const val USER_ID = 1
        private const val USERNAME = "testUser"
        private const val EMAIL = "test@example.com"
    }

    private val userRepository: UserRepository = mock()
    private val banRepository: BanRepository = mock()
    private val nameRecordRepository: NameRecordRepository = mock()
    private val loginLogRepository: LoginLogRepository = mock()
    private val accountLinkRepository: AccountLinkRepository = mock()
    private val gamePlayerStatsRepository: GamePlayerStatsRepository = mock()
    private val leaderboardRatingRepository: LeaderboardRatingRepository = mock()
    private val uniqueIdUserRepository: UniqueIdUserRepository = mock()

    private val service = DatabaseAccountAnonymizationService(
        userRepository = userRepository,
        banRepository = banRepository,
        nameRecordRepository = nameRecordRepository,
        loginLogRepository = loginLogRepository,
        accountLinkRepository = accountLinkRepository,
        gamePlayerStatsRepository = gamePlayerStatsRepository,
        leaderboardRatingRepository = leaderboardRatingRepository,
        uniqueIdUserRepository = uniqueIdUserRepository,
    )

    @Test
    fun anonymizeUserWithGamesKeepsRatingHistoryAndStillHandlesServiceLinks() {
        mockExistingUser(games = 5, bans = 0)

        val event = service.anonymizeUser(USER_ID)

        assertThat(event.userId, equalTo(USER_ID))
        assertThat(event.username, equalTo(USERNAME))
        assertThat(event.email, equalTo(EMAIL))

        verify(gamePlayerStatsRepository).countByPlayerId(USER_ID)
        verify(banRepository).countByPlayerId(USER_ID)
        verify(loginLogRepository).deleteByUserId(USER_ID)
        verify(nameRecordRepository).deleteByUserId(USER_ID)
        verify(uniqueIdUserRepository).deleteByUserId(USER_ID)
        verify(userRepository).anonymizeUser(USER_ID)
        verify(accountLinkRepository).anonymizeForDeletedUser(USER_ID)

        // rating history is only removed when the user has no games
        verify(leaderboardRatingRepository, never()).deleteByLoginId(USER_ID)
    }

    @Test
    fun anonymizeUserWithoutGamesDeletesRatingHistoryAndStillHandlesServiceLinks() {
        mockExistingUser(games = 0, bans = 0)

        val event = service.anonymizeUser(USER_ID)

        assertThat(event.userId, equalTo(USER_ID))
        assertThat(event.username, equalTo(USERNAME))
        assertThat(event.email, equalTo(EMAIL))

        verify(gamePlayerStatsRepository).countByPlayerId(USER_ID)
        verify(banRepository).countByPlayerId(USER_ID)
        verify(loginLogRepository).deleteByUserId(USER_ID)
        verify(nameRecordRepository).deleteByUserId(USER_ID)
        verify(uniqueIdUserRepository).deleteByUserId(USER_ID)
        verify(userRepository).anonymizeUser(USER_ID)
        verify(accountLinkRepository).anonymizeForDeletedUser(USER_ID)
        verify(leaderboardRatingRepository).deleteByLoginId(USER_ID)
    }

    @Test
    fun anonymizeUserWithBansStillAnonymizesAccount() {
        mockExistingUser(games = 5, bans = 2)

        val event = service.anonymizeUser(USER_ID)

        assertThat(event.userId, equalTo(USER_ID))
        assertThat(event.username, equalTo(USERNAME))
        assertThat(event.email, equalTo(EMAIL))

        verify(gamePlayerStatsRepository).countByPlayerId(USER_ID)
        verify(banRepository).countByPlayerId(USER_ID)
        verify(loginLogRepository).deleteByUserId(USER_ID)
        verify(nameRecordRepository).deleteByUserId(USER_ID)
        verify(uniqueIdUserRepository).deleteByUserId(USER_ID)
        verify(userRepository).anonymizeUser(USER_ID)
        verify(accountLinkRepository).anonymizeForDeletedUser(USER_ID)
    }

    @Test
    fun anonymizeUserThrowsWhenUserDoesNotExist() {
        whenever(userRepository.findById(USER_ID)).thenReturn(null)

        try {
            service.anonymizeUser(USER_ID)
        } catch (exception: AccountAnonymizationUserNotFoundException) {
            assertThat(exception.message, containsString("user id $USER_ID"))

            // nothing should be touched if the user was never found
            verify(gamePlayerStatsRepository, never()).countByPlayerId(USER_ID)
            verify(banRepository, never()).countByPlayerId(USER_ID)
            verify(userRepository, never()).anonymizeUser(USER_ID)
            verify(loginLogRepository, never()).deleteByUserId(USER_ID)
            verify(accountLinkRepository, never()).anonymizeForDeletedUser(USER_ID)
            return
        }

        error("Expected AccountAnonymizationUserNotFoundException")
    }

    private fun mockExistingUser(games: Long, bans: Long) {
        val user = User(
            id = USER_ID,
            username = USERNAME,
            password = "irrelevant-hash",
            email = EMAIL,
            ip = null,
            acceptedTos = null,
        )

        whenever(userRepository.findById(USER_ID)).thenReturn(user)
        whenever(gamePlayerStatsRepository.countByPlayerId(USER_ID)).thenReturn(games)
        whenever(banRepository.countByPlayerId(USER_ID)).thenReturn(bans)
    }
}
