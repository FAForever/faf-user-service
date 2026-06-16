package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.account.UsernameValidator
import com.faforever.userservice.backend.domain.NameRecord
import com.faforever.userservice.backend.domain.NameRecordRepository
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.OffsetDateTime

@ApplicationScoped
class UcpUsernameService(
    private val userRepository: UserRepository,
    private val nameRecordRepository: NameRecordRepository,
    private val fafProperties: FafProperties,
) {
    sealed interface UsernameChangeResult {
        data class Success(val userId: Int, val newUsername: String) : UsernameChangeResult
        data class ValidationError(val message: String) : UsernameChangeResult
        data object NotLoggedIn : UsernameChangeResult
    }

    @Transactional
    fun changeUsername(currentUser: UcpUser?, newUsername: String): UsernameChangeResult {
        val user = currentUser ?: return UsernameChangeResult.NotLoggedIn

        val trimmedUsername = newUsername.trim()
        val now = OffsetDateTime.now()

        // Validate new username
        if (trimmedUsername.isEmpty()) {
            return UsernameChangeResult.ValidationError("ucp.username.error.empty")
        }

        if (!UsernameValidator.startsWithLetter(trimmedUsername)) {
            return UsernameChangeResult.ValidationError("ucp.username.error.mustStartWithLetter")
        }

        if (!UsernameValidator.hasValidLength(trimmedUsername)) {
            return UsernameChangeResult.ValidationError("ucp.username.error.length")
        }

        if (!UsernameValidator.containsOnlyAllowedCharacters(trimmedUsername)) {
            return UsernameChangeResult.ValidationError("ucp.username.error.invalidCharacters")
        }

        if (trimmedUsername.equals(user.userName, ignoreCase = true)) {
            return UsernameChangeResult.ValidationError("ucp.username.error.sameAsCurrent")
        }

        // Check username-change cooldown
        if (fafProperties.account().username().minimumDaysBetweenUsernameChange() > 0) {
            val cooldownStart = now.minusDays(
                fafProperties.account().username().minimumDaysBetweenUsernameChange().toLong(),
            )
            if (nameRecordRepository.existsByUserIdAndChangeTimeAfter(currentUser.userId, cooldownStart)) {
                return UsernameChangeResult.ValidationError("ucp.username.error.cooldown")
            }
        }

        // Check if username is taken by another user
        if (userRepository.existsByUsername(trimmedUsername)) {
            return UsernameChangeResult.ValidationError("ucp.username.error.taken")
        }

        // Check if username is reserved by someone else (not the current user)
        val reservationCutoff = now.minusMonths(
            fafProperties.account().username().usernameReservationTimeInMonths(),
        )
        val reservedByOtherUser = nameRecordRepository.existsByPreviousNameAndChangeTimeAfterAndUserIdNotEquals(
            trimmedUsername,
            reservationCutoff,
            user.userId,
        )

        if (reservedByOtherUser) {
            return UsernameChangeResult.ValidationError("ucp.username.error.reserved")
        }

        val previousUsername = user.userName
        userRepository.updateUsername(user.userId, trimmedUsername)
        nameRecordRepository.persist(
            NameRecord(
                userId = user.userId,
                changeTime = now,
                previousName = previousUsername,
            ),
        )

        return UsernameChangeResult.Success(user.userId, trimmedUsername)
    }
}
