package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.AvatarAssignmentRepository
import com.faforever.userservice.backend.domain.AvatarRepository
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

data class AccountData(
    val userId: Int,
    val username: String,
    val email: String,
    val avatarUrl: String?,
    val avatarTooltip: String?,
)

data class UserAvatar(
    val id: Int,
    val tooltip: String?,
    val url: String?,
    val isSelected: Boolean,
)

sealed interface AvatarSelectionResult {
    data object Success : AvatarSelectionResult
    data object AvatarExpired : AvatarSelectionResult
}

@ApplicationScoped
class UcpAccountDataService(
    private val userRepository: UserRepository,
    private val avatarAssignmentRepository: AvatarAssignmentRepository,
    private val avatarRepository: AvatarRepository,
) {
    @Transactional
    fun getAccountData(userId: Int): AccountData {
        val user = requireNotNull(userRepository.findById(userId)) {
            "Expected authenticated UCP user with id '$userId' to exist"
        }
        val persistedUserId = requireNotNull(user.id) {
            "User '$userId' has no persistent id"
        }

        val equippedAvatar = avatarAssignmentRepository.findSelectedAvatarByUserId(userId)
        if (equippedAvatar == null) {
            // Clean up any expired selected avatars for when the equipped avatar has expired and none is selected
            avatarAssignmentRepository.findExpiredSelectedAvatarByUserId(userId)?.let {
                it.selected = false
            }
        }
        val avatarDetails = equippedAvatar?.let { avatar ->
            avatarRepository.findById(avatar.idAvatar)
        }

        return AccountData(
            userId = persistedUserId,
            username = user.username,
            email = user.email,
            avatarUrl = avatarDetails?.url,
            avatarTooltip = avatarDetails?.tooltip,
        )
    }

    fun getAvailableAvatars(userId: Int): List<UserAvatar> {
        val userAvatarAssignments = avatarAssignmentRepository.findAllByUserId(userId)
        return userAvatarAssignments.mapNotNull { assignment ->
            avatarRepository.findById(assignment.idAvatar)?.let { avatar ->
                UserAvatar(
                    id = avatar.id,
                    tooltip = avatar.tooltip,
                    url = avatar.url,
                    isSelected = assignment.selected,
                )
            }
        }
    }

    @Transactional
    fun selectAvatar(userId: Int, avatarId: Int): AvatarSelectionResult {
        avatarAssignmentRepository.findAllByUserIdIncludingExpired(userId)
            .forEach { it.selected = false }

        val selectedAssignment = avatarAssignmentRepository.findAssignmentByUserIdAndAvatarId(userId, avatarId)
        if (selectedAssignment != null) {
            selectedAssignment.selected = true
            return AvatarSelectionResult.Success
        }
        return AvatarSelectionResult.AvatarExpired
    }

    @Transactional
    fun deselectAvatar(userId: Int) {
        avatarAssignmentRepository.findAllByUserIdIncludingExpired(userId)
            .forEach { it.selected = false }
    }
}
