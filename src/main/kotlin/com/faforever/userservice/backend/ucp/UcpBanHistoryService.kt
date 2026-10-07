package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Ban
import com.faforever.userservice.backend.domain.BanLevel
import com.faforever.userservice.backend.domain.BanRepository
import jakarta.enterprise.context.ApplicationScoped
import java.time.OffsetDateTime

enum class UcpBanStatus {
    ACTIVE,
    EXPIRED,
    REVOKED,
}

data class UcpBanHistoryEntry(
    val id: Int,
    val level: BanLevel,
    val reason: String,
    val createTime: OffsetDateTime,
    val expiresAt: OffsetDateTime?,
    val status: UcpBanStatus,
    val revokeTime: OffsetDateTime?,
    val revokeReason: String?,
)

@ApplicationScoped
class UcpBanHistoryService(
    private val banRepository: BanRepository,
) {
    fun getBanHistoryForUser(userId: Int): List<UcpBanHistoryEntry> =
        banRepository.findByPlayerIdOrderByCreateTimeDesc(userId)
            .map { ban -> ban.toHistoryEntry() }

    private fun Ban.toHistoryEntry() = UcpBanHistoryEntry(
        id = id,
        level = level,
        reason = reason,
        createTime = createTime,
        expiresAt = expiresAt,
        status = getStatus(),
        revokeTime = revokeTime,
        revokeReason = revokeReason,
    )

    private fun Ban.getStatus(): UcpBanStatus = when {
        revokeTime != null -> UcpBanStatus.REVOKED
        isActive -> UcpBanStatus.ACTIVE
        else -> UcpBanStatus.EXPIRED
    }
}
