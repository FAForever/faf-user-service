package com.faforever.userservice.backend.email

import com.faforever.userservice.backend.domain.DomainBlacklistRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

@ApplicationScoped
class EmailService(
    private val userRepository: UserRepository,
    private val domainBlacklistRepository: DomainBlacklistRepository,
    private val properties: FafProperties,
    private val mailSender: MailSender,
    private val mailBodyBuilder: MailBodyBuilder,
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(EmailService::class.java)
        private val EMAIL_PATTERN: Pattern = Pattern.compile(".+@.+\\..+$")
    }

    enum class ValidationResult {
        VALID,
        INVALID,
        BLACKLISTED,
    }

    fun changeUserEmail(newEmail: String, user: User) {
        validateEmailAddress(newEmail)
        log.debug("Changing email for user '${user.username}' to '$newEmail'")
        val updatedUser = user.copy(email = newEmail)
        userRepository.persist(updatedUser)
        // TODO: broadcastUserChange(user)
    }

    /**
     * Checks whether the specified email address as a valid format and its domain is not blacklisted.
     */
    @Transactional
    fun validateEmailAddress(email: String) = when {
        !EMAIL_PATTERN.matcher(email).matches() -> ValidationResult.INVALID

        domainBlacklistRepository.existsByDomain(
            email.substring(email.lastIndexOf('@') + 1),
        ) -> ValidationResult.BLACKLISTED

        else -> ValidationResult.VALID
    }

    fun sendActivationMail(username: String, email: String, activationUrl: String) {
        val mailBody = mailBodyBuilder.buildAccountActivationBody(username, activationUrl)
        mailSender.sendMail(email, properties.account().registration().subject(), mailBody, ContentType.HTML)
    }

    fun sendWelcomeToFafMail(username: String, email: String) {
        val mailBody = mailBodyBuilder.buildWelcomeToFafBody(username)
        mailSender.sendMail(email, properties.account().registration().welcomeSubject(), mailBody, ContentType.HTML)
    }

    fun sendPasswordResetMail(username: String, email: String, passwordResetUrl: String) {
        val mailBody = mailBodyBuilder.buildPasswordResetBody(username, passwordResetUrl)
        mailSender.sendMail(email, properties.account().passwordReset().subject(), mailBody, ContentType.HTML)
    }

    fun sendEmailAlreadyTakenMail(
        desiredUsername: String,
        existingUsername: String,
        email: String,
        passwordResetUrl: String,
    ) {
        val mailBody = mailBodyBuilder.buildEmailTakenBody(desiredUsername, existingUsername, passwordResetUrl)
        mailSender.sendMail(email, properties.account().registration().emailTakenSubject(), mailBody, ContentType.HTML)
    }
}
