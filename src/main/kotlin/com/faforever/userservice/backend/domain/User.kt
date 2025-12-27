package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity(name = "login")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "login")
    var username: String,
    var password: String,
    var email: String,
    val ip: String?,
    @Column(name = "accepted_tos")
    var acceptedTos: Short?,
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
    val userId: Int?,
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
    val id: Int,
    @Column(name = "technical_name")
    val technicalName: String,
    @Column(name = "create_time")
    @CreationTimestamp
    val createTime: LocalDateTime,
    @Column(name = "update_time")
    @UpdateTimestamp
    val updateTime: LocalDateTime,
) : PanacheEntityBase

@Entity(name = "terms_of_service")
data class TermsOfService(
    @Id
    @GeneratedValue
    val version: Short,
    @Column(name = "valid_from", nullable = false)
    val validFrom: LocalDateTime,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    val content: String,
) : PanacheEntityBase

@ApplicationScoped
class UserRepository : PanacheRepositoryBase<User, Int> {
    fun findByUsernameOrEmail(usernameOrEmail: String): User? =
        find("username = ?1 or email = ?1", usernameOrEmail).firstResult()

    fun findByEmail(email: String): User? =
        find("email = ?1", email).firstResult()

    fun findUserPermissions(userId: Int): List<Permission> =
        getEntityManager().createNativeQuery(
            """
            SELECT DISTINCT group_permission.* FROM user_group_assignment uga
            INNER JOIN group_permission_assignment gpa ON uga.group_id = gpa.group_id
            INNER JOIN group_permission ON gpa.permission_id = group_permission.id
            WHERE uga.user_id = :userId
            """.trimIndent(),
            Permission::class.java,
        ).setParameter("userId", userId)
            .resultList as List<Permission>

    fun existsByUsername(username: String): Boolean = count("username = ?1", username) > 0

    fun existsByEmail(email: String): Boolean = count("email = ?1", email) > 0

    fun findBySteamId(steamId: String): User? =
        getEntityManager().createNativeQuery(
            """
            SELECT login.*
            FROM login
            INNER JOIN service_links ON login.id = service_links.user_id
            WHERE type = 'STEAM' and service_id = :steamId
            """.trimIndent(),
            User::class.java,
        ).setParameter("steamId", steamId)
            .resultList.firstOrNull() as User?
}

@ApplicationScoped
class AccountLinkRepository : PanacheRepositoryBase<AccountLink, String> {
    fun hasOwnershipLink(userId: Int): Boolean =
        count("userId = ?1 and ownership", userId) > 0
}

@ApplicationScoped
class TermsOfServiceRepository : PanacheRepositoryBase<TermsOfService, Short> {
    fun findLatest(): TermsOfService? = find("validFrom <= ?1 order by version desc", LocalDateTime.now()).firstResult()
}
