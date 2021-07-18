package com.faforever.userservice

import com.faforever.userservice.domain.Ban
import com.faforever.userservice.domain.BanLevel
import com.faforever.userservice.domain.BanRepository
import com.faforever.userservice.domain.FailedAttemptsSummary
import com.faforever.userservice.domain.LoginLogRepository
import com.faforever.userservice.domain.User
import com.faforever.userservice.domain.UserRepository
import com.faforever.userservice.security.FafPasswordEncoder
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
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
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

        private val user = User(1, username, password, email, null, null)
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
        webTestClient
            .get()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String::class.java)
    }

    @Test
    fun postLoginWithUnknownUser() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.empty())
        `when`(loginLogRepository.save(anyOrNull())).thenAnswer { Mono.just(it.arguments[0]) }
        `when`(loginLogRepository.findFailedAttemptsByIp(anyString()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))

        mockLoginRequest()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location("/login?login_challenge=someChallenge&login_challenge=someChallenge&login_failed")
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(loginLogRepository).save(anyOrNull())
        verify(loginLogRepository).findFailedAttemptsByIp(anyString())
    }

    @Test
    fun postLoginWithThrottling() {
        `when`(loginLogRepository.findFailedAttemptsByIp(any()))
            .thenReturn(Mono.just(FailedAttemptsSummary(100, 1, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().minusSeconds(10))))

        mockLoginRequest()
        mockLoginReject()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location("/login?login_challenge=someChallenge&login_challenge=someChallenge&login_throttled")
            .expectBody(String::class.java)

        verify(loginLogRepository).findFailedAttemptsByIp(anyString())
    }

    @Test
    fun postLoginWithInvalidPassword() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches(password, password)).thenReturn(false)
        `when`(loginLogRepository.findFailedAttemptsByIp(anyString()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }

        mockLoginRequest()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location("/login?login_challenge=someChallenge&login_challenge=someChallenge&login_failed")
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIp(anyString())
        verify(loginLogRepository).save(anyOrNull())
    }

    @Test
    fun postLoginWithBannedUser() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIp(anyString()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull()))
            .thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.just(
                Ban(1, 1, BanLevel.CHAT, "test", LocalDateTime.MIN, null),
                Ban(1, 1, BanLevel.GLOBAL, "test", LocalDateTime.MAX, null),
            )
        )

        mockLoginRequest()
        mockLoginReject()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIp(anyString())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun postLoginWithUnbannedUser() {
        `when`(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches(password, password)).thenReturn(true)
        `when`(loginLogRepository.findFailedAttemptsByIp(anyString()))
            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
        `when`(loginLogRepository.save(anyOrNull())).thenAnswer { Mono.just(it.arguments[0]) }
        `when`(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
            Flux.just(
                Ban(1, 1, BanLevel.CHAT, "test", LocalDateTime.MIN, null),
                Ban(1, 1, BanLevel.GLOBAL, "test", LocalDateTime.MAX, LocalDateTime.now().minusDays(1)),
            )
        )

        mockLoginRequest()
        mockLoginAccept()

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("usernameOrEmail", username)
                    .with("password", password)
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)

        verify(userRepository).findByUsernameOrEmail(username, username)
        verify(passwordEncoder).matches(password, password)
        verify(loginLogRepository).findFailedAttemptsByIp(anyString())
        verify(loginLogRepository).save(anyOrNull())
        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
    }

    @Test
    fun getConsent() {
        `when`(userRepository.findById(1)).thenReturn(Mono.just(user))

        mockConsentRequest()

        webTestClient
            .get()
            .uri("/consent?consent_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isOk()
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
            .uri("/consent?consent_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("consent_challenge", challenge)
                    .with("consent_challenge", challenge)
                    .with("action", "permit")
            )
            .exchange()
            .expectStatus().is3xxRedirection()
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
            .uri("/consent?consent_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("consent_challenge", challenge)
                    .with("consent_challenge", challenge)
                    .with("action", "deny")
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location(hydraRedirectUrl)
            .expectBody(String::class.java)
    }

    private fun mockLoginRequest() {
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
                        "requested_scope": [],
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
}
