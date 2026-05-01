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

@Entity(name = "avatars_list")
data class AvatarList(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    val tooltip: String,
    @CreationTimestamp
    @Column(name = "create_time")
    val createTime: LocalDateTime?,
    @UpdateTimestamp
    @Column(name = "update_time")
    val updateTime: LocalDateTime?,
    val filename: String,
    val url: String,
    @Column(name = "avatar_text_description")
    val avatarTextDescription: String,
) : PanacheEntityBase

@Entity(name = "avatars")
data class Avatar(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,
    @Column(name = "iduser")
    val idUser: Int,
    @Column(name = "idavatar")
    val idAvatar: Int,
    val selected: Boolean,
) : PanacheEntityBase

@ApplicationScoped
class AvatarListRepository : PanacheRepositoryBase<AvatarList, Int>

@ApplicationScoped
class AvatarRepository : PanacheRepositoryBase<Avatar, Int> {
    fun findSelectedAvatarByUserId(userId: Int): Avatar? =
        find("idUser = ?1 and selected = true", userId).firstResult()
}