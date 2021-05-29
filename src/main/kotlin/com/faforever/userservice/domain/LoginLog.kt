package com.faforever.userservice.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Table("login_log")
data class LoginLog(
    @Id
    val id: Long,
    @Column("login_id")
    val userId: Long?,
    @Column("login_string")
    val loginString: String?,
    val ip: String,
    val success: Boolean,
    @CreatedDate
    val createTime: LocalDateTime = LocalDateTime.now(),
)

data class FailedAttemptsSummary(
    val totalAttempts: Long?,
    val accountsAffected: Long?,
    val firstAttemptAt: LocalDateTime?,
    val lastAttemptAt: LocalDateTime?,
)

@Repository
interface LoginLogRepository : ReactiveCrudRepository<LoginLog, Long> {
    @Query(
        """SELECT
            count(*) as total_attempts,
            count(DISTINCT login_id) as accounts_affected,
            min(create_time) as first_attempt_at,
            max(create_time) as last_attempt_at
        FROM login_log WHERE ip = :ip AND success = 0
    """
    )
    fun findFailedAttemptsByIp(ip: String): Mono<FailedAttemptsSummary>
}
