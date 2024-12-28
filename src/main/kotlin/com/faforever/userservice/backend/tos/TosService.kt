package com.faforever.userservice.backend.tos

import com.faforever.userservice.backend.domain.TermsOfService
import com.faforever.userservice.backend.domain.TermsOfServiceRepository
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped


@ApplicationScoped
class TosService(
    private val userRepository: UserRepository,
    private val tosRepository: TermsOfServiceRepository,
) {
    fun hasUserAcceptedLatestTos(userId: Int): Boolean {
        val user = userRepository.findById(userId)
        user?.let {
            val latestTos = tosRepository.findLatest()
            return latestTos?.version == user.acceptedTos
        }
        return false
    }

    fun findLatestTos(): TermsOfService? = tosRepository.findLatest()

    fun acceptLatestTos(userId: Int) {
        val user = userRepository.findById(userId)
        user?.let {
            val latestTos = tosRepository.findLatest()
            user.acceptedTos = latestTos?.version
            userRepository.persist(user)
        }
    }
}
