package com.faforever.userservice.domain

import io.micronaut.data.annotation.DateCreated
import io.micronaut.data.annotation.DateUpdated
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.Query
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.r2dbc.annotation.R2dbcRepository
import io.micronaut.data.repository.reactive.ReactorCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime

@MappedEntity("login")
data class User(
    @field:Id
    val id: Long,
    @field:MappedProperty("login")
    val username: String,
    val password: String,
    val email: String,
    val ip: String?
) {

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "User(id=$id, username='$username')"
}

@MappedEntity("service_links")
data class AccountLink(
    @field:Id
    val id: String,
    @field:MappedProperty("user_id")
    val userId: Long?,
    val ownership: Boolean
) {

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "AccountLink(id=$id)"
}

@MappedEntity("group_permission")
data class Permission(
    @field:Id
    val id: Long,
    val technicalName: String,
    @field:DateCreated
    val createTime: LocalDateTime = LocalDateTime.now(),
    @field:DateUpdated
    val updateTime: LocalDateTime = LocalDateTime.now()
)

@R2dbcRepository(dialect = Dialect.MYSQL)
interface UserRepository : ReactorCrudRepository<User, Int> {
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

@R2dbcRepository(dialect = Dialect.MYSQL)
interface AccountLinkRepository : ReactorCrudRepository<AccountLink, String> {
    fun existsByUserIdAndOwnership(userId: Long, ownership: Boolean): Mono<Boolean>
}
