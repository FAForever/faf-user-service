package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity(name = "login_log")
class LoginLog() : PanacheEntityBase {

    constructor(
        id: Long,
        userId: Int?,
        loginString: String?,
        ip: String,
        success: Boolean
    ) : this() {
        this.id = id
        this.userId = userId
        this.loginString = loginString
        this.ip = ip
        this.success = success
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    @Column(name = "login_id")
    var userId: Int? = null
    @Column(name = "login_string")
    var loginString: String? = null
    lateinit var ip: String
    var success: Boolean = false
    @CreationTimestamp
    @Column(name = "create_time")
    lateinit var createTime: LocalDateTime
}

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
        getEntityManager().createQuery(
            """
                        SELECT new com.faforever.userservice.backend.domain.FailedAttemptsSummary(
                            count(e.id),
                            count(DISTINCT e.userId),
                            min(e.createTime),
                            max(e.createTime)
                        ) FROM com.faforever.userservice.backend.domain.LoginLog e WHERE e.ip = :ip AND e.success = false AND e.createTime >= :date
                    """,
            FailedAttemptsSummary::class.java
        )
            .setParameter("ip", ip)
            .setParameter("date", date)
            .resultStream
            .findFirst()
            .orElse(null) as? FailedAttemptsSummary
}
