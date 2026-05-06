package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Group
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UcpGroupsService(private val userRepository: UserRepository) {
    fun getGroupsForUser(userId: Int): List<Group> =
        userRepository.findUserGroups(userId)
}
