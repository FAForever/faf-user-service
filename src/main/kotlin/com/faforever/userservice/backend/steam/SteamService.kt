package com.faforever.userservice.backend.steam

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import com.faforever.userservice.config.FafProperties
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.core.UriBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandlers

@ApplicationScoped
class SteamService(
    private val fafProperties: FafProperties,
    private val userRepository: UserRepository,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(SteamService::class.java)
        const val OPENID_IDENTITY_KEY = "openid.identity"
    }

    fun buildLoginUrl(redirectUrl: String): String {
        LOG.debug("Building steam account url for redirect url: {}", redirectUrl)

        return UriBuilder.fromUri(fafProperties.steam().loginUrlFormat())
            .queryParam("openid.ns", "http://specs.openid.net/auth/2.0")
            .queryParam("openid.mode", "checkid_setup")
            .queryParam("openid.return_to", redirectUrl)
            .queryParam("openid.realm", fafProperties.steam().realm())
            .queryParam("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select")
            .queryParam("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select")
            .build().toString()
    }

    sealed interface ParsingResult {
        data object NoSteamIdPresent : ParsingResult
        data object InvalidRedirect : ParsingResult
        data class ExtractedId(val steamId: String) : ParsingResult
    }

    fun parseSteamIdFromRequestParameters(parameters: Map<String, List<String>>): ParsingResult =
        when {
            !isValidSteamRedirect(parameters) -> ParsingResult.InvalidRedirect
            else -> {
                LOG.trace("Parsing steam id from request parameters: {}", parameters)
                parameters[OPENID_IDENTITY_KEY]?.firstOrNull()
                    ?.let { identityUrl -> identityUrl.substring(identityUrl.lastIndexOf("/") + 1) }
                    ?.let { steamId ->
                        ParsingResult.ExtractedId(steamId).also {
                            LOG.debug("Extracted Steam id: {}", steamId)
                        }
                    }
                    ?: ParsingResult.NoSteamIdPresent.also {
                        LOG.debug("No OpenID identity key present")
                    }
            }
        }

    private fun isValidSteamRedirect(parameters: Map<String, List<String>>): Boolean {
        LOG.debug("Checking valid OpenID 2.0 redirect against Steam API, parameters: {}", parameters)

        val uriBuilder = UriBuilder.fromUri(fafProperties.steam().loginUrlFormat())
        parameters.forEach { uriBuilder.queryParam(it.key, it.value.first()) }
        uriBuilder.replaceQueryParam("openid.mode", "check_authentication")

        // for some reason the + character doesn't get encoded
        LOG.debug("Verification uri: {}", uriBuilder.build())

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(uriBuilder.build())
            .GET()
            .build()

        val response: HttpResponse<String> = client.send(request, BodyHandlers.ofString())
        val result = response.body()

        if (result == null || !result.contains("is_valid:true")) {
            LOG.debug("Could not verify steam redirect for identity: {}", parameters[OPENID_IDENTITY_KEY])
            return false
        } else {
            LOG.debug("Steam response successfully validated.")
            return true
        }
    }

    fun findUserBySteamId(steamId: String): User? = userRepository.findBySteamId(steamId)
}
