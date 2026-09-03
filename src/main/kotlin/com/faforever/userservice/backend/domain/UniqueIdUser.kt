package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "unique_id_users")
data class UniqueIdUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "user_id")
    val userId: Int,
    @Column(name = "uniqueid_hash")
    val uniqueIdHash: String,
) : PanacheEntityBase

@ApplicationScoped
class UniqueIdUserRepository : PanacheRepositoryBase<UniqueIdUser, Int> {
    fun deleteByUserId(userId: Int): Long = delete("userId", userId)
}
