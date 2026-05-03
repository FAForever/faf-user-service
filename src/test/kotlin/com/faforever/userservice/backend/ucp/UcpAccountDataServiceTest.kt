package com.faforever.userservice.backend.ucp

import com.faforever.userservice.backend.domain.Avatar
import com.faforever.userservice.backend.domain.AvatarList
import com.faforever.userservice.backend.domain.AvatarListRepository
import com.faforever.userservice.backend.domain.AvatarRepository
import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever

@QuarkusTest
class UcpAccountDataServiceTest {

    companion object {
        private const val USER_ID = 1

        private val USER = User(
            id = USER_ID,
            username = "Dostya",
            password = "vodka",
            email = "dostya@cybran.example.com",
            ip = null,
            acceptedTos = null,
        )

        private val AVATAR = Avatar(id = 1, idUser = USER_ID, idAvatar = 2, selected = true)

        private val AVATAR_LIST = AvatarList(
            id = 2,
            tooltip = "UEF",
            createTime = null,
            updateTime = null,
            filename = "UEF.png",
            url = "https://content.faforever.com/faf/avatars/UEF.png",
            avatarTextDescription = null,
        )
    }

    @Inject
    private lateinit var ucpAccountDataService: UcpAccountDataService

    @InjectMock
    private lateinit var userRepository: UserRepository

    @InjectMock
    private lateinit var avatarRepository: AvatarRepository

    @InjectMock
    private lateinit var avatarListRepository: AvatarListRepository

    @Test
    fun returnsNullForUnknownUser() {
        val result = ucpAccountDataService.getAccountData(USER_ID)

        assertNull(result)
    }

    @Test
    fun returnsAccountDataWithoutAvatar() {
        whenever(userRepository.findById(USER_ID)).thenReturn(USER)

        val result = requireNotNull(ucpAccountDataService.getAccountData(USER_ID))

        assertEquals(USER.username, result.username)
        assertEquals(USER.email, result.email)
        assertNull(result.avatarUrl)
        assertNull(result.avatarTooltip)
    }

    @Test
    fun returnsAccountDataWithAvatar() {
        whenever(userRepository.findById(USER_ID)).thenReturn(USER)
        whenever(avatarRepository.findSelectedAvatarByUserId(USER_ID)).thenReturn(AVATAR)
        whenever(avatarListRepository.findById(AVATAR.idAvatar)).thenReturn(AVATAR_LIST)

        val result = requireNotNull(ucpAccountDataService.getAccountData(USER_ID))

        assertEquals(AVATAR_LIST.url, result.avatarUrl)
        assertEquals(AVATAR_LIST.tooltip, result.avatarTooltip)
    }
}
