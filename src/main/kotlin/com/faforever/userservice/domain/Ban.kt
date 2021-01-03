package com.faforever.userservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDateTime

enum class BanLevel {
    GLOBAL,
    CHAT,
    VAULT,
}

@Table("ban")
data class Ban(
    @Id
    val id: Int,
    val playerId: Int,
    val level: BanLevel,
    val reason: String,
    val expiresAt: LocalDateTime?,
    val revokeTime: LocalDateTime?,
) {

    val isActive: Boolean
        get() = revokeTime == null && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()))
}

@Repository
interface BanRepository : ReactiveCrudRepository<Ban, Int> {
    fun findAllByPlayerIdAndLevel(playerId: Int, level: BanLevel): Flux<Ban>
}
