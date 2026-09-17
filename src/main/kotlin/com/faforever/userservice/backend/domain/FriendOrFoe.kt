package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.runtime.annotations.RegisterForReflection
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.io.Serializable

enum class SocialStatus {
    FRIEND,
    FOE,
}

@Embeddable
data class FriendOrFoeId(
    @Column(name = "user_id")
    val userId: Int = 0,
    @Column(name = "subject_id")
    val subjectId: Int = 0,
) : Serializable

@Entity(name = "friends_and_foes")
data class FriendOrFoe(
    @EmbeddedId
    val id: FriendOrFoeId,
    @Enumerated(EnumType.STRING)
    var status: SocialStatus,
) : PanacheEntityBase

@RegisterForReflection
data class FriendOrFoeEntry(
    val subjectId: Int,
    val username: String,
)

@ApplicationScoped
class FriendOrFoeRepository : PanacheRepositoryBase<FriendOrFoe, FriendOrFoeId> {
    fun findByUserIdAndSubjectId(userId: Int, subjectId: Int): FriendOrFoe? =
        find("id.userId = ?1 and id.subjectId = ?2", userId, subjectId).firstResult()

    fun findEntriesByUserIdAndStatus(userId: Int, status: SocialStatus): List<FriendOrFoeEntry> =
        getEntityManager().createQuery(
            """
            SELECT new com.faforever.userservice.backend.domain.FriendOrFoeEntry(
                f.id.subjectId,
                u.username
            )
            FROM friends_and_foes f
            JOIN login u ON u.id = f.id.subjectId
            WHERE f.id.userId = :userId AND f.status = :status
            ORDER BY u.username
            """.trimIndent(),
            FriendOrFoeEntry::class.java,
        ).setParameter("userId", userId)
            .setParameter("status", status)
            .resultList

    fun deleteByUserIdAndSubjectId(userId: Int, subjectId: Int): Boolean =
        delete("id.userId = ?1 and id.subjectId = ?2", userId, subjectId) > 0
}
