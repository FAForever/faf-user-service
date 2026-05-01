package com.faforever.userservice.backend.ucp
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.AvatarListRepository
import com.faforever.userservice.backend.domain.AvatarRepository
import com.faforever.userservice.backend.domain.UserRepository
import jakarta.enterprise.context.ApplicationScoped

data class AccountData(
    val userId: Int,
    val username: String,
    val email: String,
    val avatarUrl: String?,
)

@ApplicationScoped
class UcpAccountDataService (
    private val userRepository: UserRepository,
    private val avatarRepository: AvatarRepository,
    private val avatarListRepository: AvatarListRepository
){

    fun getAccountData(userId: Int): AccountData? {
        val user = userRepository.findById(userId) ?: return null
        
        // Get equipped avatar (where selected = true)
        val equippedAvatar = avatarRepository.findSelectedAvatarByUserId(userId)
        val avatarUrl = equippedAvatar?.let { avatar ->
            avatarListRepository.findById(avatar.idAvatar)?.url
        }
        
        return AccountData(
            userId = user.id ?: return null,
            username = user.username,
            email = user.email,
            avatarUrl = avatarUrl,
        )
    }
}