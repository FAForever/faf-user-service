package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity(name = "login")
data class User (
    @field:Id
    @field:GeneratedValue
    val id: Long,
    @field:Column(name = "login")
    val username: String,
    val password: String,
    val email: String,
    val ip: String?,
) {
    override fun toString(): String =
        // Do NOT expose personal information here!!
        "User(id=$id, username='$username')"
}


@Entity(name = "service_links")
data class AccountLink(
    @field:Id
    @field:GeneratedValue
    val id: String,
    @field:Column(name = "user_id")
    val userId: Long?,
    val ownership: Boolean,
) {

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "AccountLink(id=$id)"
}

@Entity(name = "group_permission")
data class Permission(
    @field:Id
    @field:GeneratedValue
    val id: Long,
    val technicalName: String,
    @field:CreationTimestamp
    val createTime: LocalDateTime = LocalDateTime.now(),
    @field:UpdateTimestamp
    val updateTime: LocalDateTime = LocalDateTime.now(),
)

@ApplicationScoped
class UserRepository : PanacheRepository<User> {
    fun findByUsernameOrEmail(usernameOrEmail: String): User? =
        find("username = ?1 or email = ?1", usernameOrEmail).firstResult()

    fun findUserPermissions(userId: Int): List<Permission> =
        getEntityManager().createNativeQuery(
            """
            SELECT DISTINCT group_permission.* FROM user_group_assignment uga
            INNER JOIN group_permission_assignment gpa ON uga.group_id = gpa.group_id
            INNER JOIN group_permission ON gpa.permission_id = group_permission.id
            WHERE uga.user_id = :userId;
            """.trimIndent(), Permission::class.java
        ).apply {
            setParameter("userId", userId)
        }.resultList as List<Permission>
}

@ApplicationScoped
class AccountLinkRepository: PanacheRepositoryBase<AccountLink, String> {
    fun hasOwnershipLink(userId: Long): Boolean =
        find("userId = ?1 and ownership").firstResult() != null
}