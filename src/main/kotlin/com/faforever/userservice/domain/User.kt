package com.faforever.userservice.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Transient
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@Table("login")
data class User(
    @Id
    val id: Long,
    @Column("login")
    val username: String,
    val password: String,
    val email: String,
    val ip: String?,
    @Column("steamid")
    val steamId: Long?,
    val gogId: String?,
) {

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "User(id=$id, username='$username')"

    @Transient
    val hasGameOwnershipVerified = steamId != null || gogId != null
}

@Table("group_permission")
data class Permission(
    @Id
    val id: Long,
    val technicalName: String,
    @CreatedDate
    val createTime: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate
    val updateTime: LocalDateTime = LocalDateTime.now(),
)

@Repository
interface UserRepository : ReactiveCrudRepository<User, Long> {
    fun findByUsernameOrEmail(username: String?, email: String?): Mono<User>

    @Query(
        """
        SELECT DISTINCT group_permission.* FROM user_group_assignment uga
        INNER JOIN group_permission_assignment gpa ON uga.group_id = gpa.group_id
        INNER JOIN group_permission ON gpa.permission_id = group_permission.id
        WHERE uga.user_id = :userId;
    """
    )
    fun findUserPermissions(userId: Int): Flux<Permission>
}
