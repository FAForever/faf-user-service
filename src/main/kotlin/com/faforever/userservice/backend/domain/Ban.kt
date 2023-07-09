package com.faforever.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import java.time.OffsetDateTime

enum class BanLevel {
    GLOBAL,
    CHAT,
    VAULT,
}

@Entity(name = "ban")
data class Ban(
    @field:Id
    @field:GeneratedValue
    val id: Long,
    val playerId: Long,
    val authorId: Long,
    val level: BanLevel,
    val reason: String,
    val expiresAt: OffsetDateTime?,
    val revokeTime: OffsetDateTime?,
    val reportId: Long?,
    val revokeReason: String?,
    val revokeAuthorId: Long?,
) {

    val isActive: Boolean
        get() = revokeTime == null && (expiresAt?.isAfter(OffsetDateTime.now()) == true)
}

@ApplicationScoped
class BanRepository : PanacheRepository<Ban> {
    fun findGlobalBansByPlayerId(playerId: Long) =
        find("playerId = ?1 and level = BanLevel.GLOBAL").list()
}
