package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.AccountLink
import com.faforever.userservice.backend.domain.AccountLinkRepository
import com.faforever.userservice.backend.domain.LinkedServiceType
import com.faforever.userservice.backend.metrics.MetricHelper
import com.faforever.userservice.backend.security.FafToken
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.steam.SteamService
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.UUID

@ApplicationScoped
class UcpSteamLinkService(
    private val steamService: SteamService,
    private val accountLinkRepository: AccountLinkRepository,
    private val fafTokenService: FafTokenService,
    private val fafProperties: FafProperties,
    private val metricHelper: MetricHelper,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(UcpSteamLinkService::class.java)
    }

    sealed interface SteamLinkStatus {
        data class Linked(val steamId: String) : SteamLinkStatus
        data object NotLinked : SteamLinkStatus
    }

    sealed interface LinkResult {
        data object Success : LinkResult
        data object Failed : LinkResult
        data object NoGameOwnership : LinkResult
        data object AlreadyLinkedToOther : LinkResult
    }

    fun getStatus(userId: Int): SteamLinkStatus {
        val link = accountLinkRepository.findByUserIdAndType(userId, LinkedServiceType.STEAM)
        val steamId = link?.serviceId
        return if (steamId.isNullOrBlank()) SteamLinkStatus.NotLinked else SteamLinkStatus.Linked(steamId)
    }

    fun buildSteamLinkUrl(userId: Int): String {
        val token = fafTokenService.createToken(
            FafToken.LinkToSteam(userId = userId),
            Duration.ofHours(6),
        )
        metricHelper.incrementUserSteamLinkRequestedCounter()
        val redirectUrl = fafProperties.steam().linkToSteamRedirectUrlFormat().format(token)
        return steamService.buildLoginUrl(redirectUrl)
    }

    @Transactional
    fun linkToSteam(parameters: Map<String, List<String>>): LinkResult {
        val token = parameters["token"]?.firstOrNull()
        val userId = token?.let {
            try {
                fafTokenService.getToken(FafToken.LinkToSteam::class, it).userId
            } catch (e: Exception) {
                LOG.debug("Invalid LINK_TO_STEAM token", e)
                null
            }
        }

        // bad/expired token or they're already linked
        if (userId == null || accountLinkRepository.findByUserIdAndType(userId, LinkedServiceType.STEAM) != null) {
            metricHelper.incrementUserSteamLinkFailedCounter()
            return LinkResult.Failed
        }

        val steamId = when (
            val result = steamService.parseSteamIdFromRequestParameters(parameters)
        ) {
            is SteamService.ParsingResult.ExtractedId -> result.steamId
            else -> null
        }
        if (steamId == null) {
            metricHelper.incrementUserSteamLinkFailedCounter()
            return LinkResult.Failed
        }

        if (!steamService.ownsForgedAlliance(steamId)) {
            metricHelper.incrementUserSteamLinkFailedCounter()
            return LinkResult.NoGameOwnership
        }

        // any existing link for this Steam ID is someone elses
        if (accountLinkRepository.findByServiceIdAndType(steamId, LinkedServiceType.STEAM) != null) {
            metricHelper.incrementUserSteamLinkFailedCounter()
            return LinkResult.AlreadyLinkedToOther
        }

        accountLinkRepository.persist(
            AccountLink(
                id = generateLinkId(),
                userId = userId,
                type = LinkedServiceType.STEAM,
                serviceId = steamId,
                isPublic = false,
                ownership = true,
            ),
        )
        metricHelper.incrementUserSteamLinkDoneCounter()
        LOG.info("Linked user {} to Steam", userId)
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
