package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.AccountLink
import com.faforever.userservice.backend.domain.AccountLinkRepository
import com.faforever.userservice.backend.domain.LinkedServiceType
import com.faforever.userservice.backend.gog.GogService
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@QuarkusTest
class UcpGogLinkServiceTest {

    companion object {
        private const val USER_ID = 42
        private const val GOG_USERNAME = "gogUser"
    }

    @Inject
    private lateinit var ucpGogLinkService: UcpGogLinkService

    @InjectMock
    private lateinit var gogService: GogService

    @InjectMock
    private lateinit var accountLinkRepository: AccountLinkRepository

    @Test
    fun linkToGog_success() {
        val token = ucpGogLinkService.buildGogToken(USER_ID)
        whenever(accountLinkRepository.findByUserIdAndType(USER_ID, LinkedServiceType.GOG)).thenReturn(null)
        whenever(accountLinkRepository.findByServiceIdAndType(GOG_USERNAME, LinkedServiceType.GOG)).thenReturn(null)
        whenever(accountLinkRepository.findById(any())).thenReturn(null)
        whenever(gogService.profileContainsToken(GOG_USERNAME, token)).thenReturn(true)
        whenever(gogService.ownsForgedAlliance(GOG_USERNAME)).thenReturn(true)

        val result = ucpGogLinkService.linkToGog(USER_ID, GOG_USERNAME)

        assertEquals(UcpGogLinkService.LinkResult.Success, result)
        val accountLinkCaptor = argumentCaptor<AccountLink>()
        verify(accountLinkRepository).persist(accountLinkCaptor.capture())
        val link = accountLinkCaptor.firstValue
        assertEquals(USER_ID, link.userId)
        assertEquals(LinkedServiceType.GOG, link.type)
        assertEquals(GOG_USERNAME, link.serviceId)
        assertEquals(false, link.isPublic)
        assertEquals(true, link.ownership)
    }

    @Test
    fun linkToGog_invalidUsername() {
        val result = ucpGogLinkService.linkToGog(USER_ID, "user@name")

        assertEquals(UcpGogLinkService.LinkResult.InvalidUsername, result)
        verify(gogService, never()).profileContainsToken(any(), any())
        verify(gogService, never()).ownsForgedAlliance(any())
        verify(accountLinkRepository, never()).persist(any<AccountLink>())
    }

    @Test
    fun linkToGog_profileTokenNotSet() {
        val token = ucpGogLinkService.buildGogToken(USER_ID)
        whenever(accountLinkRepository.findByUserIdAndType(USER_ID, LinkedServiceType.GOG)).thenReturn(null)
        whenever(accountLinkRepository.findByServiceIdAndType(GOG_USERNAME, LinkedServiceType.GOG)).thenReturn(null)
        whenever(gogService.profileContainsToken(GOG_USERNAME, token)).thenReturn(false)

        val result = ucpGogLinkService.linkToGog(USER_ID, GOG_USERNAME)

        assertEquals(UcpGogLinkService.LinkResult.ProfileTokenNotSet, result)
        verify(gogService, never()).ownsForgedAlliance(any())
        verify(accountLinkRepository, never()).persist(any<AccountLink>())
    }

    @Test
    fun linkToGog_noGameOwnership() {
        val token = ucpGogLinkService.buildGogToken(USER_ID)
        whenever(accountLinkRepository.findByUserIdAndType(USER_ID, LinkedServiceType.GOG)).thenReturn(null)
        whenever(accountLinkRepository.findByServiceIdAndType(GOG_USERNAME, LinkedServiceType.GOG)).thenReturn(null)
        whenever(gogService.profileContainsToken(GOG_USERNAME, token)).thenReturn(true)
        whenever(gogService.ownsForgedAlliance(GOG_USERNAME)).thenReturn(false)

        val result = ucpGogLinkService.linkToGog(USER_ID, GOG_USERNAME)

        assertEquals(UcpGogLinkService.LinkResult.NoGameOwnership, result)
        verify(accountLinkRepository, never()).persist(any<AccountLink>())
    }

    @Test
    fun linkToGog_alreadyLinkedToOther() {
        whenever(accountLinkRepository.findByUserIdAndType(USER_ID, LinkedServiceType.GOG)).thenReturn(null)
        whenever(accountLinkRepository.findByServiceIdAndType(GOG_USERNAME, LinkedServiceType.GOG)).thenReturn(
            AccountLink(
                id = "existing",
                userId = 99,
                type = LinkedServiceType.GOG,
                serviceId = GOG_USERNAME,
                isPublic = false,
                ownership = true,
            ),
        )

        val result = ucpGogLinkService.linkToGog(USER_ID, GOG_USERNAME)

        assertEquals(UcpGogLinkService.LinkResult.AlreadyLinkedToOther, result)
        verify(gogService, never()).profileContainsToken(any(), any())
        verify(gogService, never()).ownsForgedAlliance(any())
        verify(accountLinkRepository, never()).persist(any<AccountLink>())
    }

    @Test
    fun linkToGog_alreadyLinked() {
        whenever(accountLinkRepository.findByUserIdAndType(USER_ID, LinkedServiceType.GOG)).thenReturn(
            AccountLink(
                id = "existing",
                userId = USER_ID,
                type = LinkedServiceType.GOG,
                serviceId = GOG_USERNAME,
                isPublic = false,
                ownership = true,
            ),
        )

        val result = ucpGogLinkService.linkToGog(USER_ID, GOG_USERNAME)

        assertEquals(UcpGogLinkService.LinkResult.Failed, result)
        verify(gogService, never()).profileContainsToken(any(), any())
        verify(gogService, never()).ownsForgedAlliance(any())
        verify(accountLinkRepository, never()).persist(any<AccountLink>())
    }
}
