package com.faforever.userservice

import com.faforever.userservice.domain.Ban
import com.faforever.userservice.domain.BanLevel
import com.faforever.userservice.domain.BanRepository
import com.faforever.userservice.domain.FailedAttemptsSummary
import com.faforever.userservice.domain.LoginLogRepository
import com.faforever.userservice.domain.User
import com.faforever.userservice.domain.UserRepository
import com.faforever.userservice.hydra.RevokeRefreshTokensRequest
import com.faforever.userservice.security.FafPasswordEncoder
import com.faforever.userservice.security.FafRole
import com.faforever.userservice.security.FafScope
import com.faforever.userservice.security.OAuthRole
import com.faforever.userservice.security.OAuthScope
import com.nimbusds.jose.shaded.json.JSONArray
import com.nimbusds.jose.shaded.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockserver.integration.ClientAndServer
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class UserServiceApplicationTests {

    companion object {

        private val mockServerPort = Random.nextInt(10_000, 65_500)
        private val baseUrl = "http://localhost:$mockServerPort"
        private const val challenge = "someChallenge"
        private const val username = "someUsername"
        private const val email = "some@email.com"
        private const val password = "somePassword"
        private const val hydraRedirectUrl = "someHydraRedirectUrl"
        private val revokeRequest = RevokeRefreshTokensRequest("1", null, true)

        private val user = User(1, username, password, email, null, 0, null)
        private val mockServer = ClientAndServer(mockServerPort)

        @JvmStatic
        @DynamicPropertySource
        fun setupProperties(registry: DynamicPropertyRegistry) {
            registry.add("hydra.baseUrl") { baseUrl }
        }
    }

    @Autowired
    private lateinit var context: ApplicationContext

    @MockBean
    private lateinit var userRepository: UserRepository

    @MockBean
    private lateinit var loginLogRepository: LoginLogRepository

    @MockBean
    private lateinit var banRepository: BanRepository

    @MockBean
    private lateinit var passwordEncoder: FafPasswordEncoder

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun beforeAll() {
        webTestClient = WebTestClient
            .bindToApplicationContext(context)
            .configureClient()
            .apply { _, httpHandlerBuilder, _ ->
                httpHandlerBuilder?.filters { filters -> filters.add(0, MutatorFilter()) }
            }
            .build()
    }

    @AfterEach
    fun afterEach() {
        verifyNoMoreInteractions(userRepository, loginLogRepository, banRepository, passwordEncoder)
        mockServer.reset()
    }

    @Test
    fun contextLoads() {
    }

    @Test
    fun getLogin() {
        mockLoginRequest()

        webTestClient
            .get()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
    }

    @Test
    fun postLoginWithUnknownUser() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.empty())
        `when`(loginLogRepository.save(anyOrNull())).thenAnswer { Mono.just(it.arguments[0]) }
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))

        mockLoginRequest()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location("/oauth2/login?login_challenge=someChallenge&login_challenge=someChallenge&login_failed")
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(loginLogRepository).save(anyOrNull())
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
    }

    @Test
    fun postLoginWithThrottling() {
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(any(), any()))
            .thenReturn(
                Mono.just(
                    FailedAttemptsSummary(
                        100,
                        1,
                        LocalDateTime.now().minusMinutes(1),
                        LocalDateTime.now().minusSeconds(10)
                    )
                )
            )

        mockLoginRequest()
        mockLoginReject()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location("/oauth2/login?login_challenge=someChallenge&login_challenge=someChallenge&login_throttled")
            .expectBody(String::class.java)

        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
    }

    @Test
    fun postLoginWithInvalidPassword() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches(password, password)).thenReturn(false)
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }

        mockLoginRequest()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location("/oauth2/login?login_challenge=someChallenge&login_challenge=someChallenge&login_failed")
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
        verify(loginLogRepository).save(anyOrNull())
    }

    @Test
    fun postLoginWithBannedUser() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.just(
                Ban(1, 1, 100, BanLevel.CHAT, "test", OffsetDateTime.MIN, null, null, null, null),
                Ban(1, 1, 100, BanLevel.GLOBAL, "test", OffsetDateTime.MAX, null, null, null, null),
            )
        )

        mockLoginRequest()
        mockLoginReject()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location(String.format("/oauth2/banned?expiration=%s&reason=test", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.MAX)))
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun postLoginWithNonLinkedUserWithLobbyScope() {
        val unlinkedUser = User(1, username, password, email, null, null, null)
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(unlinkedUser))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.empty()
        )

        mockLoginRequest(scopes = listOf(OAuthScope.LOBBY))
        mockLoginReject()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location("/oauth2/gameVerificationFailed")
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun postLoginWithGogLinkedUserWithLobbyScope() {
        val unlinkedUser = User(1, username, password, email, null, null, "someGogId")
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(unlinkedUser))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.empty()
        )

        mockLoginRequest(scopes = listOf(OAuthScope.LOBBY))
        mockLoginAccept()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun postLoginWithSteamLinkedUserWithLobbyScope() {
        val unlinkedUser = User(1, username, password, email, null, 123456L, null)
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(unlinkedUser))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.empty()
        )

        mockLoginRequest(scopes = listOf(OAuthScope.LOBBY))
        mockLoginAccept()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun postLoginWithNonLinkedUserWithoutLobbyScope() {
        val unlinkedUser = User(1, username, password, email, null, null, null)
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(unlinkedUser))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.empty()
        )

        mockLoginRequest()
        mockLoginAccept()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun postLoginWithUnbannedUser() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull())).thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.just(
                Ban(1, 1, 100, BanLevel.CHAT, "test", OffsetDateTime.MIN, null, null, null, null),
                Ban(1, 1, 100, BanLevel.GLOBAL, "test", OffsetDateTime.MAX, OffsetDateTime.now().minusDays(1), null, null, null),
            )
        )

        mockLoginRequest()
        mockLoginAccept()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun getConsent() {
        `when`(userRepository.findById(1)).thenReturn(Mono.just(user))

        mockConsentRequest()

        webTestClient
            .get()
            .uri("/oauth2/consent?consent_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)

        verify(userRepository).findById(1)
    }

    @Test
    fun postConsentWithPermit() {
        `when`(userRepository.findUserPermissions(1)).thenReturn(Flux.empty())

        mockConsentRequest()
        mockConsentAccept()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/consent?consent_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("action", "permit")
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)

        verify(userRepository).findUserPermissions(1)
    }

    @Test
    fun postConsentWithDeny() {
        mockConsentRequest()
        mockConsentReject()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/consent?consent_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("challenge", challenge)
                    .with("action", "deny")
            )
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)
    }

    @Test
    fun revokeRefreshToken() {
        mockConsentRevoke()

        webTestClient
            .mutateWith(
                mockJwt().authorities(
                    FafScope(OAuthScope.ADMINISTRATIVE_ACTION),
                    FafRole(OAuthRole.ADMIN_ACCOUNT_BAN),
                )
            )
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/revokeTokens")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(revokeRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
    }

    @Test
    fun cannotRevokeRefreshTokenWithoutScope() {
        mockConsentRevoke()

        webTestClient
            .mutateWith(
                mockJwt().authorities(
                    FafRole(OAuthRole.ADMIN_ACCOUNT_BAN),
                )
            )
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/revokeTokens")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(revokeRequest)
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java)
    }

    @Test
    fun cannotRevokeRefreshTokenWithoutRole() {
        mockConsentRevoke()

        webTestClient
            .mutateWith(
                mockJwt().authorities(
                    FafScope(OAuthScope.ADMINISTRATIVE_ACTION),
                )
            )
            .mutateWith(csrf())
            .post()
            .uri("/oauth2/revokeTokens")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(revokeRequest)
            .exchange()
            .expectStatus().isForbidden
            .expectBody(String::class.java)
    }

    private fun mockLoginRequest(scopes: List<String> = listOf()) {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/oauth2/auth/requests/login")
                .withQueryStringParameter("login_challenge", challenge)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(
                    """
                    {
                        "challenge": "$challenge",
                        "client": {},
                        "request_url": "someRequestUrl",
                        "requested_access_token_audience": [],
                        "requested_scope": [${scopes.joinToString("\",\"", "\"", "\"")}],
                        "skip": false,
                        "subject": "1"
                    }
                    """.trimIndent()
                )
        )
    }

    private fun mockLoginAccept() {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("PUT")
                .withPath("/oauth2/auth/requests/login/accept")
                .withQueryStringParameter("login_challenge", challenge)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(
                    """
                        {
                            "redirect_to": "$hydraRedirectUrl"
                        }
                    """.trimIndent()
                )
        )
    }

    private fun mockLoginReject() {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("PUT")
                .withPath("/oauth2/auth/requests/login/reject")
                .withQueryStringParameter("login_challenge", challenge)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(
                    """
                        {
                            "redirect_to": "$hydraRedirectUrl"
                        }
                    """.trimIndent()
                )
        )
    }

    private fun mockConsentRequest() {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/oauth2/auth/requests/consent")
                .withQueryStringParameter("consent_challenge", challenge)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(
                    """
                    {
                        "challenge": "$challenge",
                        "client": {},
                        "request_url": "someRequestUrl",
                        "requested_access_token_audience": [],
                        "requested_scope": [],
                        "skip": false,
                        "subject": "1"
                    }
                    """.trimIndent()
                )
        )
    }

    private fun mockConsentAccept() {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("PUT")
                .withPath("/oauth2/auth/requests/consent/accept")
                .withQueryStringParameter("consent_challenge", challenge)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(
                    """
                        {
                            "redirect_to": "$hydraRedirectUrl"
                        }
                    """.trimIndent()
                )
        )
    }

    private fun mockConsentReject() {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("PUT")
                .withPath("/oauth2/auth/requests/consent/reject")
                .withQueryStringParameter("consent_challenge", challenge)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json; charset=utf-8")
                .withBody(
                    """
                        {
                            "redirect_to": "$hydraRedirectUrl"
                        }
                    """.trimIndent()
                )
        )
    }

    private fun mockConsentRevoke() {
        mockServer.`when`(
            HttpRequest.request()
                .withMethod("DELETE")
                .withPath("/oauth2/auth/sessions/consent")
                .withQueryStringParameter("all", if (revokeRequest.all != null) revokeRequest.all.toString() else "")
                .withQueryStringParameter("client", if (revokeRequest.client != null) revokeRequest.client.toString() else "")
                .withQueryStringParameter("subject", revokeRequest.subject)
        ).respond(
            HttpResponse.response()
                .withStatusCode(204)
        )
    }

    private fun addFafRoles(jwtBuilder: Jwt.Builder, vararg fafRoles: String) {
        val roles = JSONArray()
        fafRoles.forEach { roles.appendElement(it) }
        jwtBuilder.claim("ext", JSONObject(mapOf("roles" to roles)))
    }

    private fun addFafScopes(jwtBuilder: Jwt.Builder, vararg fafScopes: String) {
        val scopes = JSONArray()
        fafScopes.forEach { scopes.appendElement(it) }
        jwtBuilder.claim("scp", scopes)
    }
}
