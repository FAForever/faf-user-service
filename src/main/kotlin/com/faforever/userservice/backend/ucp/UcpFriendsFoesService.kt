package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.FriendOrFoe
import com.faforever.userservice.backend.domain.FriendOrFoeEntry
import com.faforever.userservice.backend.domain.FriendOrFoeRepository
import com.faforever.userservice.backend.domain.SocialStatus
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class UcpFriendsFoesService(
    private val userRepository: UserRepository,
    private val friendOrFoeRepository: FriendOrFoeRepository,
) {
    sealed interface AddResult {
        data class Success(val entry: FriendOrFoeEntry) : AddResult
        data class ValidationError(val message: String) : AddResult
    }

    sealed interface RemoveResult {
        data object Success : RemoveResult
        data object NotFound : RemoveResult
    }

    fun getFriends(userId: Int): List<FriendOrFoeEntry> =
        friendOrFoeRepository.findEntriesByUserIdAndStatus(userId, SocialStatus.FRIEND)

    fun getFoes(userId: Int): List<FriendOrFoeEntry> =
        friendOrFoeRepository.findEntriesByUserIdAndStatus(userId, SocialStatus.FOE)

    @Transactional
    fun add(userId: Int, subjectUsername: String, status: SocialStatus): AddResult {
        val trimmedUsername = subjectUsername.trim()

        if (trimmedUsername.isEmpty()) {
            return AddResult.ValidationError("ucp.friendsFoes.error.empty")
        }

        val subject = userRepository.findByUsername(trimmedUsername)
            ?: return AddResult.ValidationError("ucp.friendsFoes.error.notFound")

        val subjectId = subject.id ?: return AddResult.ValidationError("ucp.friendsFoes.error.notFound")
        if (subjectId == userId) {
            return AddResult.ValidationError("ucp.friendsFoes.error.self")
        }

        val existing = friendOrFoeRepository.findByUserIdAndSubjectId(userId, subjectId)
        if (existing != null) {
            if (existing.status == status) {
                return AddResult.ValidationError("ucp.friendsFoes.error.alreadyAdded")
            }
            existing.status = status
        } else {
            friendOrFoeRepository.persist(FriendOrFoe(userId, subjectId, status))
        }

        return AddResult.Success(FriendOrFoeEntry(subjectId, subject.username))
    }

    @Transactional
    fun remove(userId: Int, subjectId: Int): RemoveResult =
        if (friendOrFoeRepository.deleteByUserIdAndSubjectId(userId, subjectId)) {
            RemoveResult.Success
        } else {
            RemoveResult.NotFound
        }
}
