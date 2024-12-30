package com.faforever.userservice.backend.tos

import com.faforever.userservice.backend.domain.TermsOfService
import com.faforever.userservice.backend.domain.TermsOfServiceRepository
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional


@ApplicationScoped
class TosService(
    private val userRepository: UserRepository,
    private val tosRepository: TermsOfServiceRepository,
) {
    fun hasUserAcceptedLatestTos(userId: Int): Boolean {
        val user = userRepository.findById(userId)
            ?: throw IllegalStateException("User id $userId not found")

        val latestTosVersion = tosRepository.findLatest()?.version
        return latestTosVersion == user.acceptedTos
    }

    fun findLatestTos(): TermsOfService? = tosRepository.findLatest()

    @Transactional
    fun acceptLatestTos(userId: Int) {
        val user = userRepository.findById(userId)
            ?: throw IllegalStateException("User id $userId not found")

        val latestTos = tosRepository.findLatest()
        user.acceptedTos = latestTos?.version
        userRepository.persist(user)
    }
}
