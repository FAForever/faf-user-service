package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "leaderboard_rating")
data class LeaderboardRating(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,
    @Column(name = "login_id")
    val loginId: Int,
) : PanacheEntityBase

@ApplicationScoped
class LeaderboardRatingRepository : PanacheRepositoryBase<LeaderboardRating, Int> {
    fun deleteByLoginId(loginId: Int): Long = delete("loginId", loginId)
}
