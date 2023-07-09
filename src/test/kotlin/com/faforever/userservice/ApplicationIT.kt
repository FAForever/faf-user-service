//package com.faforever.userservice
//
//import com.faforever.userservice.backend.domain.AccountLinkRepository
//import com.faforever.userservice.domain.Ban
//import com.faforever.userservice.domain.BanLevel
//import com.faforever.userservice.domain.BanRepository
//import com.faforever.userservice.domain.ConsentForm
//import com.faforever.userservice.backend.domain.FailedAttemptsSummary
//import com.faforever.userservice.domain.LoginForm
//import com.faforever.userservice.backend.domain.LoginLogRepository
//import com.faforever.userservice.backend.domain.User
//import com.faforever.userservice.backend.domain.UserRepository
//import com.faforever.userservice.backend.hydra.RevokeRefreshTokensRequest
//import com.faforever.userservice.backend.security.FafRole
//import com.faforever.userservice.backend.security.OAuthScope
//import io.micronaut.http.HttpStatus
//import io.micronaut.http.client.exceptions.HttpClientResponseException
//import io.micronaut.runtime.EmbeddedApplication
//import io.micronaut.test.annotation.MockBean
//import io.micronaut.test.extensions.junit5.annotation.MicronautTest
//import io.micronaut.test.support.TestPropertyProvider
//import jakarta.inject.Inject
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.Assertions
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.TestInstance
//import org.junit.jupiter.api.extension.ExtendWith
//import org.mockito.ArgumentMatchers.anyBoolean
//import org.mockito.ArgumentMatchers.anyLong
//import org.mockito.ArgumentMatchers.anyString
//import org.mockito.Mock
//import org.mockito.Mockito
//import org.mockito.junit.jupiter.MockitoExtension
//import org.mockito.kotlin.any
//import org.mockito.kotlin.anyOrNull
//import org.mockito.kotlin.never
//import org.mockito.kotlin.verify
//import org.mockito.kotlin.verifyNoMoreInteractions
//import org.mockito.kotlin.whenever
//import org.mockserver.integration.ClientAndServer
//import org.mockserver.model.HttpRequest
//import org.mockserver.model.HttpResponse
//import org.springframework.security.crypto.password.PasswordEncoder
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import reactor.test.StepVerifier
//import java.time.LocalDateTime
//import java.time.OffsetDateTime
//import kotlin.random.Random
//
//private const val USERNAME_OR_PASSWORD_WRONG = "Username or password was wrong"
//private const val TOO_MANY_FAILED_ATTEMPTS = "Too many of your login attempts have failed"
//private const val BANNED = "Ban expires at"
//private const val HYDRA_REDIRECT = "hydraRedirect"
//private const val BAD_OWNERSHIP = "Game Ownership Verification Missing"
//
//// Disable transactional as it does not work with R2DBC,
//// see https://github.com/micronaut-projects/micronaut-test-resources/issues/154#issuecomment-1370888403
//@MicronautTest(transactional = false)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@ExtendWith(MockitoExtension::class)
//class ApplicationIT : TestPropertyProvider {
//
//    companion object {
//
//        private val mockServerPort = Random.nextInt(10_000, 65_500)
//        private val baseUrl = "http://localhost:$mockServerPort"
//        private const val challenge = "someChallenge"
//        private const val username = "someUsername"
//        private const val email = "some@email.com"
//        private const val password = "somePassword"
//        private val hydraRedirectUrl = "$baseUrl/someHydraRedirectUrl"
//        private val revokeRequest = RevokeRefreshTokensRequest("1", null, true)
//
//        private val user = User(1, username, password, email, null)
//        private val mockServer = ClientAndServer(mockServerPort)
//    }
//
//    override fun getProperties(): Map<String, String> = mutableMapOf(
//        "faf.hydra-base-url" to baseUrl,
//    )
//
//    @AfterEach
//    fun afterEach() {
//        verifyNoMoreInteractions(userRepository, loginLogRepository, banRepository)
//        mockServer.reset()
//    }
//
//    @Mock
//    @get:MockBean(UserRepository::class)
//    lateinit var userRepository: UserRepository
//
//    @Mock
//    @get:MockBean(AccountLinkRepository::class)
//    lateinit var accountLinkRepository: AccountLinkRepository
//
//    @Mock
//    @get:MockBean(LoginLogRepository::class)
//    lateinit var loginLogRepository: LoginLogRepository
//
//    @Mock
//    @get:MockBean(BanRepository::class)
//    lateinit var banRepository: BanRepository
//
//    @Mock
//    @get:MockBean(PasswordEncoder::class)
//    lateinit var passwordEncoder: PasswordEncoder
//
//    @Inject
//    lateinit var application: EmbeddedApplication<*>
//
//    @Inject
//    lateinit var testAuthenticationFetcher: TestAuthenticationFetcher
//
//    @Inject
//    private lateinit var oAuthClient: OAuthClient
//
//    @Test
//    fun testItWorks() {
//        Assertions.assertTrue(application.isRunning)
//    }
//
//    @Test
//    fun postLoginWithUnknownUser() {
//        whenever(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.empty())
//        whenever(loginLogRepository.save(anyOrNull())).thenAnswer { Mono.just(it.arguments[0]) }
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(Mockito.anyString(), any()))
//            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
//
//        mockLoginRequest()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(USERNAME_OR_PASSWORD_WRONG)
//        }.verifyComplete()
//
//        verify(userRepository).findByUsernameOrEmail(username, username)
//        verify(loginLogRepository).save(anyOrNull())
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(Mockito.anyString(), any())
//    }
//
//    @Test
//    fun postLoginWithThrottling() {
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(any(), any()))
//            .thenReturn(
//                Mono.just(
//                    FailedAttemptsSummary(
//                        100,
//                        1,
//                        LocalDateTime.now().minusMinutes(1),
//                        LocalDateTime.now().minusSeconds(10),
//                    ),
//                ),
//            )
//
//        mockLoginRequest()
//        mockLoginReject()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(TOO_MANY_FAILED_ATTEMPTS)
//        }.verifyComplete()
//
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
//    }
//
//    @Test
//    fun postLoginWithInvalidPassword() {
//        whenever(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
//        whenever(passwordEncoder.matches(password, password)).thenReturn(false)
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
//            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
//        whenever(loginLogRepository.save(anyOrNull()))
//            .thenAnswer { Mono.just(it.arguments[0]) }
//
//        mockLoginRequest()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(USERNAME_OR_PASSWORD_WRONG)
//        }.verifyComplete()
//
//        verify(userRepository).findByUsernameOrEmail(username, username)
//        verify(passwordEncoder).matches(password, password)
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
//        verify(loginLogRepository).save(anyOrNull())
//    }
//
//    @Test
//    fun postLoginWithBannedUser() {
//        whenever(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
//        whenever(passwordEncoder.matches(password, password)).thenReturn(true)
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
//            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
//        whenever(loginLogRepository.save(anyOrNull()))
//            .thenAnswer { Mono.just(it.arguments[0]) }
//        whenever(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
//            Flux.just(
//                Ban(1, 1, 100, BanLevel.CHAT, "test", OffsetDateTime.MIN, null, null, null, null),
//                Ban(1, 1, 100, BanLevel.GLOBAL, "test", OffsetDateTime.MAX, null, null, null, null),
//            ),
//        )
//
//        mockLoginRequest()
//        mockLoginReject()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(BANNED)
//        }.verifyComplete()
//
//        verify(userRepository).findByUsernameOrEmail(username, username)
//        verify(passwordEncoder).matches(password, password)
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
//        verify(loginLogRepository).save(anyOrNull())
//        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
//    }
//
//    @Test
//    fun postLoginWithLinkedUserWithLobbyScope() {
//        val linkedUser = User(1, username, password, email, null)
//        whenever(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(linkedUser))
//        whenever(passwordEncoder.matches(password, password)).thenReturn(true)
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
//            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
//        whenever(loginLogRepository.save(anyOrNull()))
//            .thenAnswer { Mono.just(it.arguments[0]) }
//        whenever(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
//            Flux.empty(),
//        )
//        whenever(accountLinkRepository.existsByUserIdAndOwnership(linkedUser.id, true))
//            .thenReturn(Mono.just(true))
//
//        mockLoginRequest(scopes = listOf(OAuthScope.LOBBY))
//        mockLoginAccept()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(HYDRA_REDIRECT)
//        }.verifyComplete()
//
//        verify(accountLinkRepository).existsByUserIdAndOwnership(linkedUser.id, true)
//        verify(userRepository).findByUsernameOrEmail(username, username)
//        verify(passwordEncoder).matches(password, password)
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
//        verify(loginLogRepository).save(anyOrNull())
//        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
//    }
//
//    @Test
//    fun postLoginWithNonLinkedUserWithLobbyScope() {
//        val linkedUser = User(1, username, password, email, null)
//        whenever(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(linkedUser))
//        whenever(passwordEncoder.matches(password, password)).thenReturn(true)
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
//            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
//        whenever(loginLogRepository.save(anyOrNull()))
//            .thenAnswer { Mono.just(it.arguments[0]) }
//        whenever(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
//            Flux.empty(),
//        )
//        whenever(accountLinkRepository.existsByUserIdAndOwnership(linkedUser.id, true))
//            .thenReturn(Mono.just(false))
//
//        mockLoginRequest(scopes = listOf(OAuthScope.LOBBY))
//        mockLoginReject()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(BAD_OWNERSHIP)
//        }.verifyComplete()
//
//        verify(accountLinkRepository).existsByUserIdAndOwnership(linkedUser.id, true)
//        verify(userRepository).findByUsernameOrEmail(username, username)
//        verify(passwordEncoder).matches(password, password)
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
//        verify(loginLogRepository).save(anyOrNull())
//        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
//    }
//
//    @Test
//    fun postLoginWithNonLinkedUserWithoutLobbyScope() {
//        val unlinkedUser = User(1, username, password, email, null)
//        whenever(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(unlinkedUser))
//        whenever(passwordEncoder.matches(password, password)).thenReturn(true)
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
//            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
//        whenever(loginLogRepository.save(anyOrNull()))
//            .thenAnswer { Mono.just(it.arguments[0]) }
//        whenever(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
//            Flux.empty(),
//        )
//
//        mockLoginRequest()
//        mockLoginAccept()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(HYDRA_REDIRECT)
//        }.verifyComplete()
//
//        verify(accountLinkRepository, never()).existsByUserIdAndOwnership(anyLong(), anyBoolean())
//        verify(userRepository).findByUsernameOrEmail(username, username)
//        verify(passwordEncoder).matches(password, password)
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
//        verify(loginLogRepository).save(anyOrNull())
//        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
//    }
//
//    @Test
//    fun postLoginWithUnbannedUser() {
//        whenever(userRepository.findByUsernameOrEmail(username, username)).thenReturn(Mono.just(user))
//        whenever(passwordEncoder.matches(password, password)).thenReturn(true)
//        whenever(loginLogRepository.findFailedAttemptsByIpAfterDate(anyString(), any()))
//            .thenReturn(Mono.just(FailedAttemptsSummary(null, null, null, null)))
//        whenever(loginLogRepository.save(anyOrNull())).thenAnswer { Mono.just(it.arguments[0]) }
//        whenever(banRepository.findAllByPlayerIdAndLevel(anyLong(), anyOrNull())).thenReturn(
//            Flux.just(
//                Ban(1, 1, 100, BanLevel.CHAT, "test", OffsetDateTime.MIN, null, null, null, null),
//                Ban(
//                    1,
//                    1,
//                    100,
//                    BanLevel.GLOBAL,
//                    "test",
//                    OffsetDateTime.MAX,
//                    OffsetDateTime.now().minusDays(1),
//                    null,
//                    null,
//                    null,
//                ),
//            ),
//        )
//
//        mockLoginRequest()
//        mockLoginAccept()
//
//        StepVerifier.create(
//            oAuthClient.postLoginRequest(
//                CSRF_TOKEN,
//                LoginForm(
//                    challenge,
//                    username,
//                    password,
//                ),
//            ),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(HYDRA_REDIRECT)
//        }.verifyComplete()
//
//        verify(userRepository).findByUsernameOrEmail(username, username)
//        verify(passwordEncoder).matches(password, password)
//        verify(loginLogRepository).findFailedAttemptsByIpAfterDate(anyString(), any())
//        verify(loginLogRepository).save(anyOrNull())
//        verify(banRepository).findAllByPlayerIdAndLevel(anyLong(), anyOrNull())
//    }
//
//    @Test
//    fun getConsent() {
//        whenever(userRepository.findById(1)).thenReturn(Mono.just(user))
//
//        mockConsentRequest()
//
//        StepVerifier.create(
//            oAuthClient.getConsentRequest(challenge),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK
//        }.verifyComplete()
//
//        verify(userRepository).findById(1)
//    }
//
//    @Test
//    fun postConsentWithPermit() {
//        whenever(userRepository.findById(1)).thenReturn(Mono.just(user))
//        whenever(userRepository.findUserPermissions(1)).thenReturn(Flux.empty())
//
//        mockConsentRequest()
//        mockConsentAccept()
//
//        StepVerifier.create(
//            oAuthClient.postConsentRequest(CSRF_TOKEN, ConsentForm(challenge, "permit")),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(HYDRA_REDIRECT)
//        }.verifyComplete()
//
//        verify(userRepository).findById(1)
//        verify(userRepository).findUserPermissions(1)
//    }
//
//    @Test
//    fun postConsentWithDeny() {
//        mockConsentRequest()
//        mockConsentReject()
//        mockHydraRedirect()
//
//        StepVerifier.create(
//            oAuthClient.postConsentRequest(CSRF_TOKEN, ConsentForm(challenge, "deny")),
//        ).expectNextMatches {
//            it.status == HttpStatus.OK &&
//                it.body()!!.contains(HYDRA_REDIRECT)
//        }.verifyComplete()
//    }
//
//    @Test
//    fun canRevokeRefreshTokenWithScopeAndRole() {
//        mockConsentRevoke()
//
//        testAuthenticationFetcher.setNextAuthentications(
//            FafUserAuthentication(
//                1,
//                username,
//                listOf(OAuthScope.ADMINISTRATIVE_ACTION),
//                listOf(FafRole.ADMIN_ACCOUNT_BAN),
//                mapOf(),
//            ),
//        )
//
//        StepVerifier.create(
//            oAuthClient.revokeTokens(revokeRequest),
//        )
//            .expectNextCount(1)
//            .verifyComplete()
//    }
//
//    @Test
//    fun cannotRevokeRefreshTokenWithOnlyScope() {
//        mockConsentRevoke()
//
//        testAuthenticationFetcher.setNextAuthentications(
//            FafUserAuthentication(
//                1,
//                username,
//                listOf(OAuthScope.ADMINISTRATIVE_ACTION),
//                listOf(),
//                mapOf(),
//            ),
//        )
//
//        StepVerifier.create(
//            oAuthClient.revokeTokens(revokeRequest),
//        ).expectErrorMatches {
//            it is HttpClientResponseException &&
//                it.status == HttpStatus.FORBIDDEN
//        }.verify()
//    }
//
//    @Test
//    fun cannotRevokeRefreshTokenWithOnlyRole() {
//        mockConsentRevoke()
//
//        testAuthenticationFetcher.setNextAuthentications(
//            FafUserAuthentication(
//                1,
//                username,
//                listOf(),
//                listOf(FafRole.ADMIN_ACCOUNT_BAN),
//                mapOf(),
//            ),
//        )
//
//        StepVerifier.create(
//            oAuthClient.revokeTokens(revokeRequest),
//        ).expectErrorMatches {
//            it is HttpClientResponseException &&
//                it.status == HttpStatus.FORBIDDEN
//        }.verify()
//    }
//
//    private fun mockLoginRequest(scopes: List<String> = listOf()) {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("GET")
//                .withPath("/oauth2/auth/requests/login")
//                .withQueryStringParameter("login_challenge", challenge),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(200)
//                .withHeader("Content-Type", "application/json; charset=utf-8")
//                .withBody(
//                    """
//                    {
//                        "challenge": "$challenge",
//                        "client": {},
//                        "request_url": "someRequestUrl",
//                        "requested_access_token_audience": [],
//                        "requested_scope": [${scopes.joinToString("\",\"", "\"", "\"")}],
//                        "skip": false,
//                        "subject": "1"
//                    }
//                    """.trimIndent(),
//                ),
//        )
//    }
//
//    private fun mockLoginAccept() {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("PUT")
//                .withPath("/oauth2/auth/requests/login/accept")
//                .withQueryStringParameter("login_challenge", challenge),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(200)
//                .withHeader("Content-Type", "application/json; charset=utf-8")
//                .withBody(
//                    """
//                        {
//                            "redirect_to": "$hydraRedirectUrl"
//                        }
//                    """.trimIndent(),
//                ),
//        )
//
//        mockHydraRedirect()
//    }
//
//    private fun mockLoginReject() {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("PUT")
//                .withPath("/oauth2/auth/requests/login/reject")
//                .withQueryStringParameter("login_challenge", challenge),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(200)
//                .withHeader("Content-Type", "application/json; charset=utf-8")
//                .withBody(
//                    """
//                        {
//                            "redirect_to": "$hydraRedirectUrl"
//                        }
//                    """.trimIndent(),
//                ),
//        )
//    }
//
//    private fun mockConsentRequest() {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("GET")
//                .withPath("/oauth2/auth/requests/consent")
//                .withQueryStringParameter("consent_challenge", challenge),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(200)
//                .withHeader("Content-Type", "application/json; charset=utf-8")
//                .withBody(
//                    """
//                    {
//                        "challenge": "$challenge",
//                        "client": {},
//                        "request_url": "someRequestUrl",
//                        "requested_access_token_audience": [],
//                        "requested_scope": [],
//                        "skip": false,
//                        "subject": "1"
//                    }
//                    """.trimIndent(),
//                ),
//        )
//    }
//
//    private fun mockConsentAccept() {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("PUT")
//                .withPath("/oauth2/auth/requests/consent/accept")
//                .withQueryStringParameter("consent_challenge", challenge),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(200)
//                .withHeader("Content-Type", "application/json; charset=utf-8")
//                .withBody(
//                    """
//                        {
//                            "redirect_to": "$hydraRedirectUrl"
//                        }
//                    """.trimIndent(),
//                ),
//        )
//
//        mockHydraRedirect()
//    }
//
//    private fun mockConsentReject() {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("PUT")
//                .withPath("/oauth2/auth/requests/consent/reject")
//                .withQueryStringParameter("consent_challenge", challenge),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(200)
//                .withHeader("Content-Type", "application/json; charset=utf-8")
//                .withBody(
//                    """
//                        {
//                            "redirect_to": "$hydraRedirectUrl"
//                        }
//                    """.trimIndent(),
//                ),
//        )
//    }
//
//    private fun mockConsentRevoke() {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("DELETE")
//                .withPath("/oauth2/auth/sessions/consent")
//                .withQueryStringParameter("all", if (revokeRequest.all != null) revokeRequest.all.toString() else "")
//                .apply {
//                    if (revokeRequest.client != null) {
//                        withQueryStringParameter(
//                            "client",
//                            revokeRequest.client.toString(),
//                        )
//                    }
//                }
//                .withQueryStringParameter("subject", revokeRequest.subject),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(204),
//        )
//    }
//
//    private fun mockHydraRedirect() {
//        mockServer.`when`(
//            HttpRequest.request()
//                .withMethod("GET")
//                .withPath("/someHydraRedirectUrl"),
//        ).respond(
//            HttpResponse.response()
//                .withStatusCode(200)
//                .withHeader("Content-Type", "application/json; charset=utf-8")
//                .withBody(HYDRA_REDIRECT),
//        )
//    }
//}
