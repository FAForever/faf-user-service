package com.faforever.userservice.domain

import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Transient
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorCrudRepository
import reactor.core.publisher.Flux
import java.time.OffsetDateTime

enum class BanLevel {
    GLOBAL,
    CHAT,
    VAULT,
}

@MappedEntity("ban")
data class Ban(
    @field:Id
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

    @get:Transient
    val isActive: Boolean
        get() = revokeTime == null && (expiresAt == null || expiresAt.isAfter(OffsetDateTime.now()))
}

@R2dbcRepository(dialect = Dialect.MYSQL)
interface BanRepository : ReactorCrudRepository<Ban, Long> {
    fun findAllByPlayerIdAndLevel(playerId: Long, level: BanLevel): Flux<Ban>
}
