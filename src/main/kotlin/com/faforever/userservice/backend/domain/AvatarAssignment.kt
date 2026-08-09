package com.faforever.userservice.backend.domain

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Entity(name = "avatars_list")
data class Avatar(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    val tooltip: String?,
    @CreationTimestamp
    @Column(name = "create_time")
    val createTime: LocalDateTime?,
    @UpdateTimestamp
    @Column(name = "update_time")
    val updateTime: LocalDateTime?,
    val filename: String,
    @Column(insertable = false, updatable = false)
    val url: String?,
    @Column(name = "avatar_text_description")
    val avatarTextDescription: String?,
) : PanacheEntityBase

@Entity(name = "avatars")
data class AvatarAssignment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    @Column(name = "iduser")
    val idUser: Int,
    @Column(name = "idavatar")
    val idAvatar: Int,
    var selected: Boolean,
    @Column(name = "expires_at")
    val expiresAt: OffsetDateTime? = null,
) : PanacheEntityBase

@ApplicationScoped
class AvatarRepository : PanacheRepositoryBase<Avatar, Int>

@ApplicationScoped
class AvatarAssignmentRepository : PanacheRepositoryBase<AvatarAssignment, Int> {
    fun findSelectedAvatarByUserId(userId: Int): AvatarAssignment? =
        find(
            "idUser = ?1 and selected = true and (expiresAt is null or expiresAt > ?2)",
            userId,
            OffsetDateTime.now(),
        ).firstResult()

    fun findAllByUserId(userId: Int): List<AvatarAssignment> =
        find(
            "idUser = ?1 and (expiresAt is null or expiresAt > ?2) order by id",
            userId,
            OffsetDateTime.now(),
        ).list()

    fun findAssignmentByUserIdAndAvatarId(userId: Int, avatarId: Int): AvatarAssignment? =
        find(
            "idUser = ?1 and idAvatar = ?2 and (expiresAt is null or expiresAt > ?3)",
            userId,
            avatarId,
            OffsetDateTime.now(),
        ).firstResult()

    fun findExpiredSelectedAvatarByUserId(userId: Int): AvatarAssignment? =
        find(
            "idUser = ?1 and selected = true and expiresAt is not null and expiresAt <= ?2",
            userId,
            OffsetDateTime.now(),
        ).firstResult()

    fun findAllByUserIdIncludingExpired(userId: Int): List<AvatarAssignment> =
        find("idUser = ?1", userId).list()
}
