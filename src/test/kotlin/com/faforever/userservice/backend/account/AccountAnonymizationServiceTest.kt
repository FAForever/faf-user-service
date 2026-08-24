package com.faforever.userservice.backend.account

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AccountAnonymizationServiceTest {

    private companion object {
        private const val USER_ID = 1
        private const val USERNAME = "testUser"
        private const val EMAIL = "test@example.com"
    }

    private val entityManager: EntityManager = mock()
    private val createdSql = mutableListOf<String>()

    private val service = DatabaseAccountAnonymizationService(
        entityManager = entityManager,
    )

    @Test
    fun anonymizeUserWithGamesAnonymizesLoginAndKeepsLoginRow() {
        setupNativeQueryMocks(
            games = 5,
            bans = 0,
        )

        val event = service.anonymizeUser(USER_ID)

        assertThat(event.userId, equalTo(USER_ID))
        assertThat(event.username, equalTo(USERNAME))
        assertThat(event.email, equalTo(EMAIL))

        assertThat(createdSql, hasItem(containsString("DELETE FROM login_log")))
        assertThat(createdSql, hasItem(containsString("DELETE FROM name_history")))
        assertThat(createdSql, hasItem(containsString("DELETE FROM unique_id_users")))
        assertThat(createdSql, hasItem(containsString("UPDATE login")))
        assertThat(createdSql, hasItem(containsString("UPDATE service_links")))
        assertThat(createdSql, not(hasItem(containsString("DELETE FROM service_links"))))
        assertThat(createdSql, not(hasItem(containsString("DELETE FROM leaderboard_rating"))))
        assertThat(createdSql, not(hasItem(equalTo("DELETE FROM login WHERE id = :userId"))))
    }

    @Test
    fun anonymizeUserWithoutGamesDeletesRemovableDataAndKeepsAnonymizedLoginRow() {
        setupNativeQueryMocks(
            games = 0,
            bans = 0,
        )

        val event = service.anonymizeUser(USER_ID)

        assertThat(event.userId, equalTo(USER_ID))
        assertThat(event.username, equalTo(USERNAME))
        assertThat(event.email, equalTo(EMAIL))

        assertThat(createdSql, hasItem(containsString("DELETE FROM login_log")))
        assertThat(createdSql, hasItem(containsString("DELETE FROM name_history")))
        assertThat(createdSql, hasItem(containsString("DELETE FROM unique_id_users")))
        assertThat(createdSql, hasItem(containsString("UPDATE login")))
        assertThat(createdSql, hasItem(containsString("DELETE FROM service_links")))
        assertThat(createdSql, hasItem(containsString("DELETE FROM leaderboard_rating")))
        assertThat(createdSql, not(hasItem(containsString("DELETE FROM login WHERE id = :userId"))))
    }

    @Test
    fun anonymizeUserWithBansStillAnonymizesAccount() {
        setupNativeQueryMocks(
            games = 5,
            bans = 2,
        )

        val event = service.anonymizeUser(USER_ID)

        assertThat(event.userId, equalTo(USER_ID))
        assertThat(event.username, equalTo(USERNAME))
        assertThat(event.email, equalTo(EMAIL))

        assertThat(createdSql, hasItem(containsString("UPDATE login")))
    }

    @Test
    fun anonymizeUserThrowsWhenUserDoesNotExist() {
        setupNativeQueryMocksForMissingUser()

        try {
            service.anonymizeUser(USER_ID)
        } catch (exception: AccountAnonymizationUserNotFoundException) {
            assertThat(exception.message, containsString("user id $USER_ID"))
            return
        }

        error("Expected AccountAnonymizationUserNotFoundException")
    }

    private fun setupNativeQueryMocks(
        games: Long,
        bans: Long,
    ) {
        whenever(entityManager.createNativeQuery(any<String>())).thenAnswer { invocation ->
            val sql = invocation.arguments[0] as String
            createdSql.add(sql.trim())

            when {
                sql.contains("SELECT id, login, email") -> queryReturningList(
                    listOf(arrayOf(USER_ID, USERNAME, EMAIL)),
                )

                sql.contains("FROM game_player_stats") -> queryReturningSingleResult(games)

                sql.contains("FROM ban") -> queryReturningSingleResult(bans)

                else -> updateQuery()
            }
        }
    }

    private fun setupNativeQueryMocksForMissingUser() {
        whenever(entityManager.createNativeQuery(any<String>())).thenAnswer { invocation ->
            val sql = invocation.arguments[0] as String
            createdSql.add(sql.trim())

            when {
                sql.contains("SELECT id, login, email") -> queryReturningList(emptyList<Array<Any>>())
                else -> updateQuery()
            }
        }
    }

    private fun queryReturningList(result: List<Array<Any>>): Query {
        val query: Query = mock()
        whenever(query.setParameter(any<String>(), any())).thenReturn(query)
        whenever(query.resultList).thenReturn(result)
        return query
    }

    private fun queryReturningSingleResult(result: Long): Query {
        val query: Query = mock()
        whenever(query.setParameter(any<String>(), any())).thenReturn(query)
        whenever(query.singleResult).thenReturn(result)
        return query
    }

    private fun updateQuery(): Query {
        val query: Query = mock()
        whenever(query.setParameter(any<String>(), any())).thenReturn(query)
        whenever(query.executeUpdate()).thenReturn(1)
        return query
    }
}
