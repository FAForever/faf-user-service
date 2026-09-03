package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.AccountLinkRepository
import com.faforever.userservice.backend.domain.BanRepository
import com.faforever.userservice.backend.domain.GamePlayerStatsRepository
import com.faforever.userservice.backend.domain.LeaderboardRatingRepository
import com.faforever.userservice.backend.domain.LoginLogRepository
import com.faforever.userservice.backend.domain.NameRecordRepository
import com.faforever.userservice.backend.domain.UniqueIdUserRepository
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory

interface AccountAnonymizationService {
    fun anonymizeUser(userId: Int): AccountDeletedEvent
}

@ApplicationScoped
class DatabaseAccountAnonymizationService(
    private val userRepository: UserRepository,
    private val banRepository: BanRepository,
    private val nameRecordRepository: NameRecordRepository,
    private val loginLogRepository: LoginLogRepository,
    private val accountLinkRepository: AccountLinkRepository,
    private val gamePlayerStatsRepository: GamePlayerStatsRepository,
    private val leaderboardRatingRepository: LeaderboardRatingRepository,
    private val uniqueIdUserRepository: UniqueIdUserRepository,
) : AccountAnonymizationService {
    companion object {
        private val LOG = LoggerFactory.getLogger(DatabaseAccountAnonymizationService::class.java)
    }

    @Transactional
    override fun anonymizeUser(userId: Int): AccountDeletedEvent {
        val user = userRepository.findById(userId)
            ?: throw AccountAnonymizationUserNotFoundException(userId)

        val originalUsername = user.username
        val originalEmail = user.email

        val games = gamePlayerStatsRepository.countByPlayerId(userId)
        val bans = banRepository.countByPlayerId(userId)

        LOG.info("Starting account anonymization for user id {}: games={}, bans={}", userId, games, bans)
        if (bans > 0) {
            LOG.warn("Anonymizing user id {} with {} ban history entries", userId, bans)
        }

        loginLogRepository.deleteByUserId(userId)
        nameRecordRepository.deleteByUserId(userId)
        uniqueIdUserRepository.deleteByUserId(userId)
        userRepository.anonymizeUser(userId)
        accountLinkRepository.anonymizeForDeletedUser(userId)

        if (games == 0L) {
            LOG.info("User id {} has no games; deleting removable rating history", userId)
            leaderboardRatingRepository.deleteByLoginId(userId)
        } else {
            LOG.info("User id {} has games; keeping rating history", userId)
        }

        LOG.info("Completed account anonymization for user id {}", userId)

        return AccountDeletedEvent(userId = userId, username = originalUsername, email = originalEmail)
    }
}

class AccountAnonymizationUserNotFoundException(userId: Int) : RuntimeException(
    "Could not anonymize account because user id $userId was not found",
)
