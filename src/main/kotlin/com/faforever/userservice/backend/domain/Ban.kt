package com.faforever.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.*
import java.time.OffsetDateTime

enum class BanLevel {
    GLOBAL,
    CHAT,
    VAULT,
}

@Entity(name = "ban")
class Ban : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int = 0
    @Column(name = "player_id")
    var playerId: Long = 0
    @Column(name = "author_id")
    val authorId: Int = 0
    lateinit var level: BanLevel
    lateinit var reason: String
    @Column(name = "expires_at")
    var expiresAt: OffsetDateTime? = null
    @Column(name = "revoke_time")
    var revokeTime: OffsetDateTime? = null
    @Column(name = "report_id")
    var reportId: Long? = null
    @Column(name = "revoke_reason")
    var revokeReason: String? = null
    @Column(name = "revoke_author_id")
    var revokeAuthorId: Long? = null

    val isActive: Boolean
        get() = revokeTime == null && (expiresAt?.isAfter(OffsetDateTime.now()) == true)
}

@ApplicationScoped
class BanRepository : PanacheRepository<Ban> {
    fun findGlobalBansByPlayerId(playerId: Int) =
        find("playerId = ?1 and level = BanLevel.GLOBAL", playerId).list()
}
