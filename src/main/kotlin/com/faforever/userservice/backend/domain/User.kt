package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity(name = "login")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "login")
    val username: String,
    val password: String,
    val email: String,
    val ip: String?,
) : PanacheEntityBase {

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "User(id=$id, username='$username')"
}


@Entity(name = "service_links")
data class AccountLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: String,
    @Column(name = "user_id")
    val userId: Long?,
    val ownership: Boolean,
) : PanacheEntityBase {

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "AccountLink(id=$id)"
}

@Entity(name = "group_permission")
data class Permission(
    @Id
    @GeneratedValue
    val id: Long,
    @Column(name = "technical_name")
    val technicalName: String,
    @Column(name = "create_time")
    @CreationTimestamp
    val createTime: LocalDateTime,
    @Column(name = "update_time")
    @UpdateTimestamp
    val updateTime: LocalDateTime,
) : PanacheEntityBase

@ApplicationScoped
class UserRepository : PanacheRepositoryBase<User, Int> {
    fun findByUsernameOrEmail(usernameOrEmail: String): User? =
        find("username = ?1 or email = ?1", usernameOrEmail).firstResult()

    fun findUserPermissions(userId: Int): List<Permission> =
        getEntityManager().createNativeQuery(
            """
            SELECT DISTINCT group_permission.* FROM user_group_assignment uga
            INNER JOIN group_permission_assignment gpa ON uga.group_id = gpa.group_id
            INNER JOIN group_permission ON gpa.permission_id = group_permission.id
            WHERE uga.user_id = :userId
            """.trimIndent(), Permission::class.java
        ).setParameter("userId", userId)
            .resultList as List<Permission>

    fun existsByUsername(username: String): Boolean = count("username = ?1", username) > 0

    fun existsByEmail(email: String): Boolean = count("email = ?1", email) > 0
}

@ApplicationScoped
class AccountLinkRepository: PanacheRepositoryBase<AccountLink, String> {
    fun hasOwnershipLink(userId: Int): Boolean =
        count("userId = ?1 and ownership", userId) > 0
}