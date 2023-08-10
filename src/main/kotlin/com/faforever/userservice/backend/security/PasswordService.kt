package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.backend.metrics.MetricHelper
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ApplicationScoped
class PasswordService(
    private val fafProperties: FafProperties,
    private val metricHelper: MetricHelper,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(FafTokenService::class.java)
    }

    enum class ValidatePasswordResult {
        VALID,
        TOO_SHORT,
    }

    fun validatePassword(password: String) =
        if (password.length < fafProperties.security().minimumPasswordLength()) {
            ValidatePasswordResult.TOO_SHORT
        } else {
            ValidatePasswordResult.VALID
        }

    enum class ChangePasswordResult {
        OK,
        PASSWORD_MISMATCH,
    }

    fun changePassword(user: User, oldPassword: String, newPassword: String): ChangePasswordResult {
        if (!passwordEncoder.matches(oldPassword, user.passwordHash)) {
            return ChangePasswordResult.PASSWORD_MISMATCH
        }

        userRepository.persist(
            user.copy(
                passwordHash = passwordEncoder.encode(newPassword),
            ),
        )
        metricHelper.userPasswordChangeCounter.increment()

        log.info("Password of user ${user.id} was changed")

        return ChangePasswordResult.OK
    }
}
