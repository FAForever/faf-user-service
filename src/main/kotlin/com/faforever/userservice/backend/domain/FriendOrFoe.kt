package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import java.io.Serializable

enum class SocialStatus {
    FRIEND,
    FOE,
}

data class FriendOrFoeId(
    val userId: Int = 0,
    val subjectId: Int = 0,
) : Serializable

@Entity(name = "friends_and_foes")
@IdClass(FriendOrFoeId::class)
data class FriendOrFoe(
    @Id
    @Column(name = "user_id")
    val userId: Int,
    @Id
    @Column(name = "subject_id")
    val subjectId: Int,
    @Enumerated(EnumType.STRING)
    var status: SocialStatus,
) : PanacheEntityBase

@RegisterForReflection
data class FriendOrFoeEntry(
    val subjectId: Int,
    val username: String,
    val avatarUrl: String? = null,
    val avatarTooltip: String? = null,
)

@ApplicationScoped
class FriendOrFoeRepository : PanacheRepositoryBase<FriendOrFoe, FriendOrFoeId> {
    fun findByUserIdAndSubjectId(userId: Int, subjectId: Int): FriendOrFoe? =
        find("userId = ?1 and subjectId = ?2", userId, subjectId).firstResult()

    fun findEntriesByUserIdAndStatus(userId: Int, status: SocialStatus): List<FriendOrFoeEntry> =
        getEntityManager().createQuery(
            """
            SELECT new com.faforever.userservice.backend.domain.FriendOrFoeEntry(
                f.subjectId,
                u.username,
                avatar.url,
                avatar.tooltip
            )
            FROM friends_and_foes f
            JOIN login u ON u.id = f.subjectId
            LEFT JOIN avatars assignment ON assignment.idUser = f.subjectId AND assignment.selected = true
            LEFT JOIN avatars_list avatar ON avatar.id = assignment.idAvatar
            WHERE f.userId = :userId AND f.status = :status
            ORDER BY u.username
            """.trimIndent(),
            FriendOrFoeEntry::class.java,
        ).setParameter("userId", userId)
            .setParameter("status", status)
            .resultList

    fun deleteByUserIdAndSubjectId(userId: Int, subjectId: Int): Boolean =
        delete("userId = ?1 and subjectId = ?2", userId, subjectId) > 0
}
