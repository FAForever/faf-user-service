package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity(name = "login")
class User : PanacheEntityBase {
    @Id
    @GeneratedValue
    var id: Int = 0
    @Column(name = "login")
    lateinit var username: String
    lateinit var password: String
    lateinit var email: String
    var ip: String? = null

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "User(id=$id, username='$username')"
}


@Entity(name = "service_links")
class AccountLink : PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    lateinit var id: String
    @Column(name = "user_id")
    var userId: Long? = null
    var ownership: Boolean = false

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "AccountLink(id=$id)"
}

@Entity(name = "group_permission")
class Permission : PanacheEntityBase {
    @Id
    @GeneratedValue
    var id: Long = 0
    lateinit var technicalName: String
    @CreationTimestamp
    lateinit var createTime: LocalDateTime
    @UpdateTimestamp
    lateinit var updateTime: LocalDateTime
}

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
            WHERE uga.user_id = :userId;
            """.trimIndent(), Permission::class.java
        ).setParameter("userId", userId)
            .resultList as List<Permission>
}

@ApplicationScoped
class AccountLinkRepository: PanacheRepositoryBase<AccountLink, String> {
    fun hasOwnershipLink(userId: Int): Boolean =
        find("userId = ?1 and ownership", userId).firstResult() != null
}