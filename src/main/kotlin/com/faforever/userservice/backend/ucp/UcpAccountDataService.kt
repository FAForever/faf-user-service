package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.AvatarAssignmentRepository
import com.faforever.userservice.backend.domain.AvatarRepository
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped

data class AccountData(
    val userId: Int,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val avatarTooltip: String?,
)

@ApplicationScoped
class UcpAccountDataService(
    private val userRepository: UserRepository,
    private val avatarAssignmentRepository: AvatarAssignmentRepository,
    private val avatarRepository: AvatarRepository,
) {
    fun getAccountData(userId: Int): AccountData {
        val user = requireNotNull(userRepository.findById(userId)) {
            "Expected authenticated UCP user with id '$userId' to exist"
        }
        val persistedUserId = requireNotNull(user.id) {
            "User '$userId' has no persistent id"
        }

        // Get equipped avatar (where selected = true)
        val equippedAvatar = avatarAssignmentRepository.findSelectedAvatarByUserId(userId)
        val avatarDetails = equippedAvatar?.let { avatar ->
            avatarRepository.findById(avatar.idAvatar)
        }
        val avatarUrl = avatarDetails?.url
        val avatarTooltip = avatarDetails?.tooltip

        return AccountData(
            userId = persistedUserId,
            username = user.username,
            email = user.email,
            avatarUrl = avatarUrl,
            avatarTooltip = avatarTooltip,
        )
    }
}
