package com.faforever.usermanagement.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("login")
class User(
    @Id
    val id: Int,
    @Column("login")
    val username: String,
    val password: String,
    val email: String,
    val ip: String,
    @Column("steamid")
    val steamId: Int?,
)
