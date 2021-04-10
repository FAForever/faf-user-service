package com.faforever.userservice

import com.faforever.userservice.domain.User
import com.faforever.userservice.domain.UserRepository
import com.faforever.userservice.hydra.HydraProperties
import com.faforever.userservice.security.FafPasswordEncoder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
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
import kotlin.random.Random

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class UserServiceApplicationTests {

    companion object {

        private val mockServerPort = Random.nextInt(10_000, 65_500)
        private val baseUrl = "http://localhost:$mockServerPort"
        private val challenge = "someChallenge"

        private val mockServer = ClientAndServer(mockServerPort)

        @JvmStatic
        @DynamicPropertySource
        fun setupProperties(registry: DynamicPropertyRegistry) {
            registry.add("hydra.baseUrl") { baseUrl }
        }
    }

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var hydraProperties: HydraProperties

    @MockBean
    private lateinit var userRepository: UserRepository

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
        `when`(userRepository.findByUsername("foo")).thenReturn(Mono.empty())

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
                        "challenge": "someChallenge",
                        "client": {},
                        "request_url": "someRequestUrl",
                        "requested_access_token_audience": [],
                        "requested_scope": [],
                        "skip": true,
                        "subject": "1"
                    }
                    """.trimIndent()
                )
        )

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("username", "foo")
                    .with("password", "bar")
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location("/login?login_challenge=someChallenge&login_challenge=someChallenge&login_failed")
            .expectBody(String::class.java)
    }

    @Test
    fun postLoginWithInvalidPassword() {
        val user = User(1L, "foo", "invalidPassword", "some@email.com", null, null)
        `when`(userRepository.findByUsername("foo")).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches("bar", "invalidPassword")).thenReturn(false)

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

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("username", "foo")
                    .with("password", "bar")
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location("/login?login_challenge=someChallenge&login_challenge=someChallenge&login_failed")
            .expectBody(String::class.java)
    }

    @Test
    fun postLoginWithKnownUser() {
        val user = User(1L, "foo", "ignoredPassword", "some@email.com", null, null)
        `when`(userRepository.findByUsername("foo")).thenReturn(Mono.just(user))
        `when`(passwordEncoder.matches("bar", "ignoredPassword")).thenReturn(true)

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
                        "redirect_to": "someHydraRedirectUrl"
                    }
                    """.trimIndent()
                )
        )

        webTestClient
            .mutateWith(csrf())
            .post()
            .uri("/login?login_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(
                BodyInserters.fromFormData("login_challenge", challenge)
                    .with("username", "foo")
                    .with("password", "bar")
            )
            .exchange()
            .expectStatus().is3xxRedirection()
            .expectHeader()
            .location("someHydraRedirectUrl")
            .expectBody(String::class.java)
    }

    @Test
    fun getConsent() {
        val user = User(1L, "foo", "invalidPassword", "some@email.com", null, null)
        `when`(userRepository.findById(1L)).thenReturn(Mono.just(user))

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
                        "subject": "1",
                        "client": {}
                    }
                    """.trimIndent()
                )
        )

        webTestClient
            .get()
            .uri("/consent?consent_challenge=$challenge")
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isOk()
            .expectBody(String::class.java)
    }

    @Test
    fun postConsentWithPermit() {
        val user = User(1L, "foo", "invalidPassword", "some@email.com", null, null)
        `when`(userRepository.findByUsername("foo")).thenReturn(Mono.just(user))
        `when`(userRepository.findUserPermissions(1)).thenReturn(Flux.empty())

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
                        "redirect_to": "someHydraRedirectUrl"
                    }
                    """.trimIndent()
                )
        )

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
            .location("someHydraRedirectUrl")
            .expectBody(String::class.java)
    }

    @Test
    fun postConsentWithDeny() {
        val user = User(1L, "foo", "invalidPassword", "some@email.com", null, null)
        `when`(userRepository.findByUsername("foo")).thenReturn(Mono.just(user))
        `when`(userRepository.findUserPermissions(1)).thenReturn(Flux.empty())

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
                        "redirect_to": "someHydraRedirectUrl"
                    }
                    """.trimIndent()
                )
        )

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
            .location("someHydraRedirectUrl")
            .expectBody(String::class.java)
    }
}
