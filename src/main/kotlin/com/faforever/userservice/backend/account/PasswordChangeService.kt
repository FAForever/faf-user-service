package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.email.EmailService
import com.faforever.userservice.backend.security.PasswordEncoder
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory

sealed interface PasswordChangeResult {
    data object Success : PasswordChangeResult
    data object InvalidCurrentPassword : PasswordChangeResult
    data object PasswordUnchanged : PasswordChangeResult
}

@ApplicationScoped
class PasswordChangeService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(PasswordChangeService::class.java)
    }

    @Transactional
    fun changePassword(userId: Int, currentPassword: String, newPassword: String): PasswordChangeResult {
        val user = requireNotNull(userRepository.findById(userId)) {
            "Expected authenticated UCP user with id '$userId' to exist"
        }

        if (!passwordEncoder.matches(currentPassword, user.password)) {
            return PasswordChangeResult.InvalidCurrentPassword
        }

        if (passwordEncoder.matches(newPassword, user.password)) {
            return PasswordChangeResult.PasswordUnchanged
        }

        user.password = passwordEncoder.encode(newPassword)
        userRepository.persist(user)

        emailService.sendPasswordChangedNotificationMail(user.username, user.email)
        LOG.info("Password for user id {} has been changed from UCP", userId)

        return PasswordChangeResult.Success
    }
}
