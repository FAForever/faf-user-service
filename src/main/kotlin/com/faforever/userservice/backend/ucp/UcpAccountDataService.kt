package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped

data class AccountData(
    val userId: Int,
    val username: String,
    val email: String,
)

@ApplicationScoped
class UcpAccountDataService (private val userRepository: UserRepository){

    fun getAccountData(userId: Int): AccountData? {
        val user = userRepository.findById(userId) ?: return null
        
        return AccountData(
            userId = user.id ?: return null,
            username = user.username,
            email = user.email,
        )
    }
}