package com.faforever.usermanagement.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
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
    val id: Int,
    @Column("login_id")
    val userId: Int,
    val ip: String,
    val attempts: Int,
    val success: Boolean,
    @CreatedDate
    val createTime: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate
    val updateTime: LocalDateTime = LocalDateTime.now(),
)

data class FailedAttemptsSummary(
    val totalAttempts: Long?,
    val accountsAffected: Long?,
    val firstAttemptAt: LocalDateTime?,
    val lastAttemptAt: LocalDateTime?,
)

@Repository
interface LoginLogRepository : ReactiveCrudRepository<LoginLog, Int> {
    fun findByUserIdAndIp(userId: Int?, ip: String): Mono<LoginLog>
    fun findByUserIdAndIpAndSuccess(userId: Int?, ip: String, success: Boolean): Mono<LoginLog>

    @Query(
        "SELECT sum(attempts) as total_attempts, count(login_id) as accounts_affected, min(create_time) as first_attempt_at, max(update_time) as last_attempt_at " +
            "FROM login_log WHERE ip = :ip AND success = 0"
    )
    fun findFailedAttemptsByIp(ip: String): Mono<FailedAttemptsSummary>
}
