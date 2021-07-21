package com.faforever.userservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.OffsetDateTime

enum class BanLevel {
    GLOBAL,
    CHAT,
    VAULT,
}

@Table("ban")
data class Ban(
    @Id
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
        get() = revokeTime == null && (expiresAt == null || expiresAt.isAfter(OffsetDateTime.now()))
}

@Repository
interface BanRepository : ReactiveCrudRepository<Ban, Long> {
    fun findAllByPlayerIdAndLevel(playerId: Long, level: BanLevel): Flux<Ban>
}
