package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedHashMap
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WikijsClientHeadersFactoryTest {

    private val fafProperties: FafProperties = mock()
    private val wikijs: FafProperties.WikiJs = mock()
    private val jwt: FafProperties.Jwt = mock()
    private val hmac: FafProperties.Hmac = mock()
    private val factory = WikiJsClientHeadersFactory(fafProperties, HmacService())

    @Test
    fun updateAddsAuthorizationAndHmacHeadersWhenHmacConfigured() {
        whenever(fafProperties.wikijs()).thenReturn(wikijs)
        whenever(wikijs.token()).thenReturn("wikijs-token")
        whenever(fafProperties.jwt()).thenReturn(jwt)
        whenever(jwt.hmac()).thenReturn(hmac)
        whenever(hmac.message()).thenReturn("helloFaf")
        whenever(hmac.secret()).thenReturn("banana")

        val headers = factory.update(MultivaluedHashMap(), MultivaluedHashMap())

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION), equalTo("Bearer wikijs-token"))
        assertThat(headers.getFirst("X-HMAC"), containsString("-"))
    }

    @Test
    fun updateOmitsHmacHeaderWhenHmacNotConfigured() {
        whenever(fafProperties.wikijs()).thenReturn(wikijs)
        whenever(wikijs.token()).thenReturn("wikijs-token")
        whenever(fafProperties.jwt()).thenReturn(jwt)
        whenever(jwt.hmac()).thenReturn(null)

        val headers = factory.update(MultivaluedHashMap(), MultivaluedHashMap())

        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION), equalTo("Bearer wikijs-token"))
        assertThat(headers.getFirst("X-HMAC"), nullValue())
    }
}
