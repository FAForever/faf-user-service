package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Permission
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class UcpPermissionService(private val userRepository: UserRepository) {
    fun getPermissionsForUser(userId: Int): List<Permission> =
        userRepository.findUserPermissions(userId)
}
