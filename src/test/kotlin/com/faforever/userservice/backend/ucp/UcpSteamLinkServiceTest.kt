package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.AccountLink
import com.faforever.userservice.backend.domain.AccountLinkRepository
import com.faforever.userservice.backend.domain.LinkedServiceType
import com.faforever.userservice.backend.metrics.MetricHelper
import com.faforever.userservice.backend.security.FafToken
import com.faforever.userservice.backend.security.FafTokenService
import com.faforever.userservice.backend.steam.SteamService
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.mockito.InjectSpy
import jakarta.inject.Inject
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.temporal.TemporalAmount

@QuarkusTest
class UcpSteamLinkServiceTest {

    @Inject
    private lateinit var ucpSteamLinkService: UcpSteamLinkService

    @InjectMock
    private lateinit var accountLinkRepository: AccountLinkRepository

    @InjectMock
    private lateinit var fafTokenService: FafTokenService

    @InjectSpy
    private lateinit var steamService: SteamService

    @InjectSpy
    private lateinit var metricHelper: MetricHelper

    @Test
    fun buildSteamLinkUrl_returnsSteamOpenIdUrl() {
        whenever(
            fafTokenService.createToken(
                eq(FafToken.LinkToSteam(userId = 1)),
                any<TemporalAmount>(),
            ),
        ).thenReturn("token-abc")

        val url = ucpSteamLinkService.buildSteamLinkUrl(1)

        assertThat(url, startsWith("https://steamcommunity.com/openid/login"))
        assertThat(url, containsString("token-abc"))
        verify(metricHelper).incrementUserSteamLinkRequestedCounter()
    }

    @Test
    fun linkToSteam_success() {
        val parameters = mapOf(
            "token" to listOf("valid-token"),
            "openid.identity" to listOf("https://steamcommunity.com/openid/id/7654321"),
        )
        whenever(fafTokenService.getToken(FafToken.LinkToSteam::class, "valid-token"))
            .thenReturn(FafToken.LinkToSteam(userId = 1))
        whenever(steamService.parseSteamIdFromRequestParameters(parameters)).thenReturn(
            SteamService.ParsingResult.ExtractedId("7654321"),
        )
        whenever(steamService.ownsForgedAlliance("7654321")).thenReturn(true)
        whenever(accountLinkRepository.findByServiceIdAndType("7654321", LinkedServiceType.STEAM)).thenReturn(null)
        whenever(accountLinkRepository.findById(any())).thenReturn(null)

        val result = ucpSteamLinkService.linkToSteam(parameters)

        assertThat(result, equalTo(UcpSteamLinkService.LinkResult.Success))
        val linkCaptor = argumentCaptor<AccountLink>()
        verify(accountLinkRepository).persist(linkCaptor.capture())
        val link = linkCaptor.firstValue
        assertThat(link.userId, equalTo(1))
        assertThat(link.type, equalTo(LinkedServiceType.STEAM))
        assertThat(link.serviceId, equalTo("7654321"))
        assertThat(link.isPublic, equalTo(false))
        assertThat(link.ownership, equalTo(true))
        verify(metricHelper).incrementUserSteamLinkDoneCounter()
    }

    @Test
    fun linkToSteam_failsOnInvalidToken() {
        whenever(fafTokenService.getToken(FafToken.LinkToSteam::class, "bad-token"))
            .thenThrow(IllegalArgumentException("expired"))

        val result = ucpSteamLinkService.linkToSteam(mapOf("token" to listOf("bad-token")))

        assertThat(result, equalTo(UcpSteamLinkService.LinkResult.Failed))
        verify(accountLinkRepository, never()).persist(any<AccountLink>())
        verify(metricHelper).incrementUserSteamLinkFailedCounter()
    }

    @Test
    fun linkToSteam_failsOnInvalidRedirect() {
        val parameters = mapOf("token" to listOf("valid-token"))
        whenever(fafTokenService.getToken(FafToken.LinkToSteam::class, "valid-token"))
            .thenReturn(FafToken.LinkToSteam(userId = 1))
        whenever(steamService.parseSteamIdFromRequestParameters(parameters))
            .thenReturn(SteamService.ParsingResult.InvalidRedirect)

        val result = ucpSteamLinkService.linkToSteam(parameters)

        assertThat(result, equalTo(UcpSteamLinkService.LinkResult.Failed))
        verify(metricHelper).incrementUserSteamLinkFailedCounter()
    }

    @Test
    fun linkToSteam_noGameOwnership() {
        val parameters = mapOf("token" to listOf("valid-token"))
        whenever(fafTokenService.getToken(FafToken.LinkToSteam::class, "valid-token"))
            .thenReturn(FafToken.LinkToSteam(userId = 1))
        whenever(steamService.parseSteamIdFromRequestParameters(parameters)).thenReturn(
            SteamService.ParsingResult.ExtractedId("7654321"),
        )
        whenever(steamService.ownsForgedAlliance("7654321")).thenReturn(false)

        val result = ucpSteamLinkService.linkToSteam(parameters)

        assertThat(result, equalTo(UcpSteamLinkService.LinkResult.NoGameOwnership))
        verify(metricHelper).incrementUserSteamLinkFailedCounter()
    }

    @Test
    fun linkToSteam_alreadyLinkedToOther() {
        val parameters = mapOf("token" to listOf("valid-token"))
        whenever(fafTokenService.getToken(FafToken.LinkToSteam::class, "valid-token"))
            .thenReturn(FafToken.LinkToSteam(userId = 1))
        whenever(steamService.parseSteamIdFromRequestParameters(parameters)).thenReturn(
            SteamService.ParsingResult.ExtractedId("7654321"),
        )
        whenever(steamService.ownsForgedAlliance("7654321")).thenReturn(true)
        whenever(accountLinkRepository.findByServiceIdAndType("7654321", LinkedServiceType.STEAM)).thenReturn(
            AccountLink(id = "existing", userId = 99, type = LinkedServiceType.STEAM, serviceId = "7654321"),
        )

        val result = ucpSteamLinkService.linkToSteam(parameters)

        assertThat(result, equalTo(UcpSteamLinkService.LinkResult.AlreadyLinkedToOther))
        verify(metricHelper).incrementUserSteamLinkFailedCounter()
    }
}
