package com.faforever.userservice.backend.gog

import com.faforever.userservice.config.FafProperties
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration
import java.util.regex.Pattern

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogGame(
    val game: GogGameDetails? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogGameDetails(
    val id: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogGamesPage(
    val _embedded: GogEmbedded? = null,
    val pages: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GogEmbedded(
    val items: List<GogGame> = emptyList(),
)

@ApplicationScoped
class GogService(
    private val fafProperties: FafProperties,
    private val objectMapper: ObjectMapper,
) {
    private val httpClient: HttpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
            .build()

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GogService::class.java)
        private const val HTTP_TIMEOUT_SECONDS = 10L

        private val PROFILE_USER_STATUS_PATTERN: Pattern =
            Pattern.compile(
                """window\.profilesData\.profileUserPreferences\s*=\s*\{.*?"bio"\s*:\s*"(.*?)"""",
                Pattern.DOTALL,
            )
    }

    fun fetchProfile(gogUsername: String): String? {
        LOG.debug("Fetching GOG profile for username: {}", gogUsername)

        val profileUrl = fafProperties.gog().profileUrlFormat().format(gogUsername)
        return fetchBody(profileUrl, "GOG profile for username $gogUsername")
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

        val gamesUrl = fafProperties.gog().gamesListUrlFormat().format(gogUsername, page)
        val responseBody = fetchBody(gamesUrl, "GOG games page for username $gogUsername page $page")
            ?: return null

        return try {
            objectMapper.readValue(responseBody, GogGamesPage::class.java)
        } catch (e: Exception) {
            LOG.debug("Could not parse GOG games page for username: {}, page: {}", gogUsername, page, e)
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

    private fun fetchBody(url: String, description: String): String? {
        return try {
            val uri = URI(url)
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .timeout(Duration.ofSeconds(HTTP_TIMEOUT_SECONDS))
                .build()

            val response = httpClient.send(request, BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                response.body()
            } else {
                LOG.debug("Could not fetch {}: status code={}", description, response.statusCode())
                null
            }
        } catch (e: Exception) {
            LOG.debug("Exception fetching {}", description, e)
            null
        }
    }

    fun profileContainsToken(gogUsername: String, token: String): Boolean {
        LOG.debug("Checking if GOG profile contains token for username: {}", gogUsername)

        val profileStatus = getProfileStatus(gogUsername) ?: return false
        val profileStatusTrimmed = profileStatus.trim()

        return profileStatusTrimmed == token
    }
}
