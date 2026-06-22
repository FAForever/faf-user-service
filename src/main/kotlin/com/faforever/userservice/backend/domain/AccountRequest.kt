package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.transaction.Transactional
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity(name = "account_request")
data class AccountRequest(
    @Id
    val id: String,
    @Column(name = "user_id")
    val userId: Int?,
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    val type: AccountRequestType,
    @Column(name = "token_hash", nullable = false)
    val tokenHash: String,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: OffsetDateTime,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    val data: Map<String, Any>,
) : PanacheEntityBase

enum class AccountRequestType {
    EMAIL_CHANGE,
}

@ApplicationScoped
class AccountRequestRepository : PanacheRepositoryBase<AccountRequest, String> {
    fun deleteByUserIdAndType(userId: Int, type: AccountRequestType) {
        delete("userId = ?1 and type = ?2", userId, type)
    }

    @Transactional
    @Scheduled(every = "10m")
    fun deleteExpired() {
        delete("expiresAt <= ?1", OffsetDateTime.now())
    }
}
