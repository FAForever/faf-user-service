package com.faforever.userservice.backend.account

import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

interface AccountAnonymizationService {
    fun anonymizeUser(userId: Int): AccountDeletedEvent
}

@ApplicationScoped
class DatabaseAccountAnonymizationService(
    private val entityManager: EntityManager,
) : AccountAnonymizationService {
    companion object {
        private val LOG = LoggerFactory.getLogger(DatabaseAccountAnonymizationService::class.java)
    }

    @Transactional
    override fun anonymizeUser(userId: Int): AccountDeletedEvent {
        val target = findTarget(userId)
            ?: throw AccountAnonymizationUserNotFoundException(userId)

        LOG.info(
            "Starting account anonymization for user id {}: games={}, bans={}",
            userId,
            target.games,
            target.bans,
        )

        if (target.bans > 0) {
            LOG.warn("Anonymizing user id {} with {} ban history entries", userId, target.bans)
        }

        deleteLoginLogs(userId)
        deleteNameHistory(userId)
        deleteUniqueIdUsers(userId)
        anonymizeLogin(userId)

        if (target.games == 0L) {
            LOG.info(
                "User id {} has no games; deleting removable account data and keeping anonymized login row",
                userId,
            )
            deleteServiceLinks(userId)
            deleteLeaderboardRatings(userId)
        } else {
            LOG.info(
                "User id {} has games; keeping anonymized login row and unlinking owned service links",
                userId,
            )
            unlinkOwnedServiceLinks(userId)
        }

        LOG.info("Completed account anonymization for user id {}", userId)

        return AccountDeletedEvent(
            userId = target.userId,
            username = target.username,
            email = target.email,
        )
    }

    private fun findTarget(userId: Int): AccountDeletionTarget? {
        val rows = entityManager.createNativeQuery(
            """
            SELECT id, login, email
            FROM login
            WHERE id = :userId
            """.trimIndent(),
        )
            .setParameter("userId", userId)
            .resultList

        val row = rows.firstOrNull() as? Array<*>
            ?: return null

        return AccountDeletionTarget(
            userId = (row[0] as Number).toInt(),
            username = row[1] as String,
            email = row[2] as String,
            games = countGames(userId),
            bans = countBans(userId),
        )
    }

    private fun countGames(userId: Int): Long =
        (
            entityManager.createNativeQuery(
                """
            SELECT count(*)
            FROM game_player_stats
            WHERE playerId = :userId
                """.trimIndent(),
            )
                .setParameter("userId", userId)
                .singleResult as Number
            ).toLong()

    private fun countBans(userId: Int): Long =
        (
            entityManager.createNativeQuery(
                """
            SELECT count(*)
            FROM ban
            WHERE player_id = :userId
                """.trimIndent(),
            )
                .setParameter("userId", userId)
                .singleResult as Number
            ).toLong()

    private fun deleteLoginLogs(userId: Int) {
        entityManager.createNativeQuery("DELETE FROM login_log WHERE login_id = :userId")
            .setParameter("userId", userId)
            .executeUpdate()
    }

    private fun deleteNameHistory(userId: Int) {
        entityManager.createNativeQuery("DELETE FROM name_history WHERE user_id = :userId")
            .setParameter("userId", userId)
            .executeUpdate()
    }

    private fun deleteUniqueIdUsers(userId: Int) {
        entityManager.createNativeQuery("DELETE FROM unique_id_users WHERE user_id = :userId")
            .setParameter("userId", userId)
            .executeUpdate()
    }

    private fun anonymizeLogin(userId: Int) {
        entityManager.createNativeQuery(
            """
            UPDATE login
            SET password = 'anonymized',
                login = concat('anonymized_', id),
                email = concat('anonymized_', id),
                ip = null,
                steamid = null,
                gog_id = null,
                user_agent = null,
                last_login = null
            WHERE id = :userId
            """.trimIndent(),
        )
            .setParameter("userId", userId)
            .executeUpdate()
    }

    private fun deleteServiceLinks(userId: Int) {
        entityManager.createNativeQuery("DELETE FROM service_links WHERE user_id = :userId")
            .setParameter("userId", userId)
            .executeUpdate()
    }

    private fun deleteLeaderboardRatings(userId: Int) {
        entityManager.createNativeQuery("DELETE FROM leaderboard_rating WHERE login_id = :userId")
            .setParameter("userId", userId)
            .executeUpdate()
    }

    private fun unlinkOwnedServiceLinks(userId: Int) {
        entityManager.createNativeQuery(
            """
            UPDATE service_links
            SET user_id = null
            WHERE user_id = :userId
              AND ownership = 1
            """.trimIndent(),
        )
            .setParameter("userId", userId)
            .executeUpdate()
    }

    private data class AccountDeletionTarget(
        val userId: Int,
        val username: String,
        val email: String,
        val games: Long,
        val bans: Long,
    )
}

class AccountAnonymizationUserNotFoundException(userId: Int) : RuntimeException(
    "Could not anonymize account because user id $userId was not found",
)
