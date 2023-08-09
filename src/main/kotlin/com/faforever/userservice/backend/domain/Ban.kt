package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.OffsetDateTime

enum class BanLevel {
    GLOBAL,
    CHAT,
    VAULT,
}

@Entity(name = "ban")
data class Ban(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    @Column(name = "player_id")
    val playerId: Long,
    @Column(name = "author_id")
    val authorId: Int,
    @Enumerated(EnumType.STRING)
    val level: BanLevel,
    val reason: String,
    @Column(name = "expires_at")
    val expiresAt: OffsetDateTime?,
    @Column(name = "revoke_time")
    val revokeTime: OffsetDateTime?,
    @Column(name = "report_id")
    val reportId: Long?,
    @Column(name = "revoke_reason")
    val revokeReason: String?,
    @Column(name = "revoke_author_id")
    val revokeAuthorId: Long?,
) : PanacheEntityBase {

    val isActive: Boolean
        get() {
            val expiresAtValue = expiresAt
            return revokeTime == null && (expiresAtValue == null || expiresAtValue.isAfter(OffsetDateTime.now()))
        }
}

@ApplicationScoped
class BanRepository : PanacheRepository<Ban> {
    fun findGlobalBansByPlayerId(playerId: Int) =
        find("playerId = ?1 and level = BanLevel.GLOBAL", playerId).list()
}
