package com.faforever.userservice.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

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
) {

    override fun toString(): String =
        // Do NOT expose personal information here!!
        "User(id=$id, username='$username')"
}

@Repository
interface UserRepository : ReactiveCrudRepository<User, Long> {
    fun findByUsername(username: String?): Mono<User>
}
