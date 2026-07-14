package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.AccountLink
import com.faforever.userservice.backend.domain.AccountLinkRepository
import com.faforever.userservice.backend.domain.LinkedServiceType
import com.faforever.userservice.backend.gog.GogService
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

@ApplicationScoped
class UcpGogLinkService(
    private val gogService: GogService,
    private val accountLinkRepository: AccountLinkRepository,
    private val fafProperties: FafProperties,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(UcpGogLinkService::class.java)
        private const val MIN_GOG_USERNAME_LENGTH = 1
        private const val MAX_GOG_USERNAME_LENGTH = 18
        private val GOG_USERNAME_PATTERN = Regex("[a-zA-Z0-9._-]+")
    }

    sealed interface GogLinkStatus {
        data class Linked(val gogUsername: String) : GogLinkStatus
        data object NotLinked : GogLinkStatus
    }

    sealed interface LinkResult {
        data object Success : LinkResult
        data object Failed : LinkResult
        data object InvalidUsername : LinkResult
        data object ProfileTokenNotSet : LinkResult
        data object NoGameOwnership : LinkResult
        data object AlreadyLinkedToOther : LinkResult
    }

    fun getStatus(userId: Int): GogLinkStatus {
        val link = accountLinkRepository.findByUserIdAndType(userId, LinkedServiceType.GOG)
        val gogUsername = link?.serviceId
        return if (gogUsername.isNullOrBlank()) GogLinkStatus.NotLinked else GogLinkStatus.Linked(gogUsername)
    }

    fun buildGogToken(userId: Int): String =
        fafProperties.gog().tokenFormat().format(userId)

    @Transactional
    fun linkToGog(userId: Int, gogUsername: String): LinkResult {
        val trimmedUsername = gogUsername.trim()
        LOG.info("Linking GOG account '{}' to FAF userId={}", trimmedUsername, userId)

        if (trimmedUsername.length !in MIN_GOG_USERNAME_LENGTH..MAX_GOG_USERNAME_LENGTH ||
            !GOG_USERNAME_PATTERN.matches(trimmedUsername)
        ) {
            LOG.debug("Rejected GOG link for userId={} due to invalid username '{}'", userId, trimmedUsername)
            return LinkResult.InvalidUsername
        }

        if (accountLinkRepository.findByUserIdAndType(userId, LinkedServiceType.GOG) != null) {
            LOG.debug("Rejected GOG link for userId={} because the FAF account is already linked", userId)
            return LinkResult.Failed
        }

        if (accountLinkRepository.findByServiceIdAndType(trimmedUsername, LinkedServiceType.GOG) != null) {
            LOG.debug(
                "Rejected GOG link for userId={} because the GOG username is already linked to another account",
                userId,
            )
            return LinkResult.AlreadyLinkedToOther
        }

        val token = buildGogToken(userId)
        if (!gogService.profileContainsToken(trimmedUsername, token)) {
            LOG.debug("Rejected GOG link for userId={} because the profile token was not found", userId)
            return LinkResult.ProfileTokenNotSet
        }

        if (!gogService.ownsForgedAlliance(trimmedUsername)) {
            LOG.debug(
                "Rejected GOG link for userId={} because the GOG account does not own Forged Alliance",
                userId,
            )
            return LinkResult.NoGameOwnership
        }

        accountLinkRepository.persist(
            AccountLink(
                id = generateLinkId(),
                userId = userId,
                type = LinkedServiceType.GOG,
                serviceId = trimmedUsername,
                isPublic = false,
                ownership = true,
            ),
        )

        LOG.info("Linked user {} to GOG", userId)
        return LinkResult.Success
    }

    private fun generateLinkId(): String {
        var id = UUID.randomUUID().toString()
        while (accountLinkRepository.findById(id) != null) {
            id = UUID.randomUUID().toString()
        }
        return id
    }
}
