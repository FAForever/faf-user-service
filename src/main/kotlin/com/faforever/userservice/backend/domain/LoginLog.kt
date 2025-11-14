package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity(name = "login_log")
data class LoginLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    @Column(name = "login_id")
    val userId: Int?,
    @Column(name = "login_string")
    val loginString: String?,
    val ip: String,
    val success: Boolean,
    @CreationTimestamp
    @Column(name = "create_time")
    val createTime: LocalDateTime = LocalDateTime.now(),
) : PanacheEntityBase

@RegisterForReflection
data class FailedAttemptsSummary(
    val totalAttempts: Long,
    val accountsAffected: Long,
    val firstAttemptAt: LocalDateTime?,
    val lastAttemptAt: LocalDateTime?,
)

@ApplicationScoped
class LoginLogRepository : PanacheRepository<LoginLog> {
    fun findLastLoginTime(userId: Int?): LocalDateTime? {
        if (userId == null) {
            return null
        }
        return find("userId = ?1 order by createTime desc", userId).firstResult()?.createTime
    }

    fun findFailedAttemptsByIpAfterDate(ip: String, date: LocalDateTime): FailedAttemptsSummary? =
        getEntityManager().createQuery(
            """
                        SELECT new com.faforever.userservice.backend.domain.FailedAttemptsSummary(
                            count(e.id),
                            count(DISTINCT e.userId),
                            min(e.createTime),
                            max(e.createTime)
                        ) FROM com.faforever.userservice.backend.domain.LoginLog e WHERE e.ip = :ip AND e.success = false AND e.createTime >= :date
                    """,
            FailedAttemptsSummary::class.java,
        )
            .setParameter("ip", ip)
            .setParameter("date", date)
            .resultStream
            .findFirst()
            .orElse(null)
}
