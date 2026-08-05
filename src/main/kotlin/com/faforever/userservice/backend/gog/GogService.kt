package com.faforever.userservice.backend.gog

import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

@ApplicationScoped
class GogService(
    private val fafProperties: FafProperties,
    @RestClient private val gogClient: GogClient,
) {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GogService::class.java)

        private val PROFILE_USER_STATUS_PATTERN: Pattern =
            Pattern.compile(
                """window\.profilesData\.profileUserPreferences\s*=\s*\{.*?"bio"\s*:\s*"(.*?)"""",
                Pattern.DOTALL,
            )
    }

    fun fetchProfile(gogUsername: String): String? {
        LOG.debug("Fetching GOG profile for username: {}", gogUsername)

        return try {
            gogClient.getProfile(gogUsername)
        } catch (e: Exception) {
            LOG.debug(
                "Could not fetch GOG profile for username: {}",
                gogUsername,
                e,
            )
            null
        }
    }

    private fun getProfileStatus(gogUsername: String): String? {
        val profileHtml = fetchProfile(gogUsername) ?: return null
        val document = Jsoup.parse(profileHtml)

        for (element in document.body().getElementsByTag("script")) {
            val scriptText = element.data()
            val matcher = PROFILE_USER_STATUS_PATTERN.matcher(scriptText)

            if (matcher.find()) {
                return matcher.group(1)
            }
        }

        return null
    }

    fun fetchGamesPage(gogUsername: String, page: Int): GogGamesPage? {
        LOG.debug("Fetching GOG games page for username: {}, page: {}", gogUsername, page)

        return try {
            gogClient.getGamesPage(
                username = gogUsername,
                sort = "recent_playtime",
                order = "desc",
                page = page,
            )
        } catch (e: Exception) {
            LOG.debug("Could not fetch GOG games page for username: {}, page: {}", gogUsername, page, e)
            null
        }
    }

    fun ownsForgedAlliance(gogUsername: String): Boolean {
        LOG.debug("Checking if GOG user owns Forged Alliance: {}", gogUsername)

        val forgedAllianceProductId = fafProperties.gog().forgedAllianceProductId()
        var currentPage = 1

        while (true) {
            val gamesPage = fetchGamesPage(gogUsername, currentPage) ?: return false

            val hasForgedAlliance = gamesPage._embedded?.items?.any { game ->
                game.game?.id == forgedAllianceProductId
            } ?: false

            if (hasForgedAlliance) {
                return true
            }

            val totalPages = gamesPage.pages ?: 0
            if (currentPage >= totalPages) {
                break
            }

            currentPage++
        }

        return false
    }

    fun profileContainsToken(gogUsername: String, token: String): Boolean {
        LOG.debug("Checking if GOG profile contains token for username: {}", gogUsername)

        val profileStatus = getProfileStatus(gogUsername) ?: return false
        val profileStatusTrimmed = profileStatus.trim()

        return profileStatusTrimmed == token
    }
}
