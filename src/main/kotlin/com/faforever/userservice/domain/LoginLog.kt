package com.faforever.userservice.domain

import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorCrudRepository
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@MappedEntity("login_log")
data class LoginLog(
    @field:Id
    val id: Long,
    @field:MappedProperty("login_id")
    val userId: Long?,
    @field:MappedProperty("login_string")
    val loginString: String?,
    val ip: String,
    val success: Boolean,
    @field:DateCreated
    val createTime: LocalDateTime = LocalDateTime.now()
)

@Introspected
data class FailedAttemptsSummary(
    val totalAttempts: Long?,
    val accountsAffected: Long?,
    val firstAttemptAt: LocalDateTime?,
    val lastAttemptAt: LocalDateTime?
)

@R2dbcRepository(dialect = Dialect.MYSQL)
interface LoginLogRepository : ReactorCrudRepository<LoginLog, Long> {
    @Query(
        """SELECT
            count(*) as totalAttempts,
            count(DISTINCT login_id) as accountsAffected,
            min(create_time) as firstAttemptAt,
            max(create_time) as lastAttemptAt
        FROM login_log WHERE ip = :ip AND success = 0 AND create_time >= :date
    """
    )
    fun findFailedAttemptsByIpAfterDate(ip: String, date: LocalDateTime): Mono<FailedAttemptsSummary>
}
