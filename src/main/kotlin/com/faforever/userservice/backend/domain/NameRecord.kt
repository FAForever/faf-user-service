package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.OffsetDateTime

@Entity(name = "name_history")
data class NameRecord(
    @Id
    val id: Int,
    @Column(name = "user_id")
    val userId: Int,
    @Column(name = "change_time")
    val changeTime: OffsetDateTime,
    @Column(name = "previous_name")
    val previousName: String,
)

@ApplicationScoped
class NameRecordRepository : PanacheRepositoryBase<NameRecord, Int> {
    fun existsByUserIdAndChangeTimeAfter(
        userId: Int,
        changeTime: OffsetDateTime,
    ): Boolean = count("userId = ?1 and changeTime >= ?2", userId, changeTime) > 0

    fun existsByPreviousNameAndChangeTimeAfter(
        previousName: String,
        changeTime: OffsetDateTime,
    ): Boolean = count("previousName = ?1 and changeTime >= ?2", previousName, changeTime) > 0

    fun existsByPreviousNameAndChangeTimeAfterAndUserIdNotEquals(
        previousName: String,
        changeTime: OffsetDateTime,
        userId: Int,
    ): Boolean = count(
        "previousName = ?1 and changeTime >= ?2 and userId != ?3",
        previousName,
        changeTime,
        userId,
    ) > 0
}
