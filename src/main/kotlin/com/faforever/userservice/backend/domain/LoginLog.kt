package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity(name = "login_log")
data class LoginLog(
    @field:Id
    @field:GeneratedValue
    val id: Long,
    @field:Column(name = "login_id")
    val userId: Long?,
    @field:Column(name = "login_string")
    val loginString: String?,
    val ip: String,
    val success: Boolean,
    @field:CreationTimestamp
    val createTime: LocalDateTime = LocalDateTime.now(),
)

@RegisterForReflection
data class FailedAttemptsSummary(
    val totalAttempts: Long,
    val accountsAffected: Long,
    val firstAttemptAt: LocalDateTime?,
    val lastAttemptAt: LocalDateTime?,
)

@ApplicationScoped
class LoginLogRepository : PanacheRepository<LoginLog> {

    fun findFailedAttemptsByIpAfterDate(ip: String, date: LocalDateTime): FailedAttemptsSummary? =
        getEntityManager().createNativeQuery(
            """
                        SELECT
                            count(*) as totalAttempts,
                            count(DISTINCT login_id) as accountsAffected,
                            min(create_time) as firstAttemptAt,
                            max(create_time) as lastAttemptAt
                        FROM login_log WHERE ip = :ip AND success = 0 AND create_time >= :date
                    """,
            FailedAttemptsSummary::class.java
        )
            .resultStream
            .findFirst()
            .orElse(null) as? FailedAttemptsSummary
}
