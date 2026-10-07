package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Ban
import com.faforever.userservice.backend.domain.BanLevel
import com.faforever.userservice.backend.domain.BanRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

@QuarkusTest
class UcpBanHistoryServiceTest {
    @Inject
    private lateinit var ucpBanHistoryService: UcpBanHistoryService

    @InjectMock
    private lateinit var banRepository: BanRepository

    @Test
    fun returnsEmptyBanHistoryWhenUserHasNoBans() {
        whenever(banRepository.findByPlayerIdOrderByCreateTimeDesc(USER_ID)).thenReturn(emptyList())

        assertThat(ucpBanHistoryService.getBanHistoryForUser(USER_ID), empty())
        verify(banRepository).findByPlayerIdOrderByCreateTimeDesc(USER_ID)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "GLOBAL, Temporary global ban, ACTIVE, 6, false",
            "CHAT, Permanent chat ban, ACTIVE, , false",
            "VAULT, Expired vault ban, EXPIRED, -2, false",
            "GLOBAL, Revoked global ban, REVOKED, 10, true",
            "CHAT, Revoked permanent chat ban, REVOKED, , true",
        ],
        nullValues = [""],
    )
    fun mapsBanStatus(
        level: BanLevel,
        reason: String,
        expectedStatus: UcpBanStatus,
        expiresInDays: Long?,
        revoked: Boolean,
    ) {
        val ban = buildBan(
            level = level,
            reason = reason,
            expiresAt = expiresInDays?.let { NOW.plusDays(it) },
            revokeTime = if (revoked) NOW.minusDays(1) else null,
            revokeReason = if (revoked) "Appeal accepted" else null,
            revokeAuthorId = if (revoked) REVOKE_AUTHOR_ID else null,
        )
        whenever(banRepository.findByPlayerIdOrderByCreateTimeDesc(USER_ID)).thenReturn(listOf(ban))

        val entry = ucpBanHistoryService.getBanHistoryForUser(USER_ID).single()
        assertThat(entry.id, equalTo(ban.id))
        assertThat(entry.level, equalTo(level))
        assertThat(entry.reason, equalTo(reason))
        assertThat(entry.status, equalTo(expectedStatus))
        assertThat(entry.expiresAt, equalTo(ban.expiresAt))
        assertThat(entry.revokeTime, equalTo(ban.revokeTime))
    }

    @Test
    fun revokedStatusTakesPrecedenceOverExpiredStatus() {
        whenever(banRepository.findByPlayerIdOrderByCreateTimeDesc(USER_ID)).thenReturn(
            listOf(
                buildBan(
                    expiresAt = NOW.minusDays(20),
                    revokeTime = NOW.minusDays(10),
                    revokeReason = "Manual revoke",
                ),
            ),
        )

        assertThat(
            ucpBanHistoryService.getBanHistoryForUser(USER_ID).single().status,
            equalTo(UcpBanStatus.REVOKED),
        )
    }

    @Test
    fun keepsRepositoryOrderingAndMapsLevels() {
        val bans = listOf(
            buildBan(id = 3, level = BanLevel.GLOBAL, reason = "Newest", createTime = NOW.minusDays(1)),
            buildBan(id = 2, level = BanLevel.CHAT, reason = "Middle", createTime = NOW.minusDays(5)),
            buildBan(id = 1, level = BanLevel.VAULT, reason = "Oldest", createTime = NOW.minusDays(20)),
        )
        whenever(banRepository.findByPlayerIdOrderByCreateTimeDesc(USER_ID)).thenReturn(bans)

        val result = ucpBanHistoryService.getBanHistoryForUser(USER_ID)
        assertThat(result.map { it.reason }, contains("Newest", "Middle", "Oldest"))
        assertThat(result.map { it.level }, contains(BanLevel.GLOBAL, BanLevel.CHAT, BanLevel.VAULT))
    }

    private fun buildBan(
        id: Int = 1,
        authorId: Int = AUTHOR_ID,
        level: BanLevel = BanLevel.GLOBAL,
        reason: String = "Test ban",
        expiresAt: OffsetDateTime? = NOW.plusDays(7),
        revokeTime: OffsetDateTime? = null,
        revokeReason: String? = null,
        revokeAuthorId: Int? = null,
        createTime: OffsetDateTime = NOW.minusDays(1),
    ) = Ban(
        id = id,
        playerId = USER_ID,
        authorId = authorId,
        level = level,
        reason = reason,
        expiresAt = expiresAt,
        revokeTime = revokeTime,
        reportId = null,
        revokeReason = revokeReason,
        revokeAuthorId = revokeAuthorId,
        createTime = createTime,
    )

    private companion object {
        const val USER_ID = 1
        const val AUTHOR_ID = 2
        const val REVOKE_AUTHOR_ID = 3
        val NOW: OffsetDateTime = OffsetDateTime.now()
    }
}
