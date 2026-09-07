package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity(name = "game_player_stats")
data class GamePlayerStats(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(name = "playerId")
    val playerId: Int,
) : PanacheEntityBase

@ApplicationScoped
class GamePlayerStatsRepository : PanacheRepositoryBase<GamePlayerStats, Long> {
    fun countByPlayerId(playerId: Int): Long = count("playerId", playerId)
}
