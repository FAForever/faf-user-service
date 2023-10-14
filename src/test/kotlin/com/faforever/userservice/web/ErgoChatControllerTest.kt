package com.faforever.userservice.web

import com.faforever.userservice.backend.hydra.HydraClient
import com.faforever.userservice.config.FafProperties
import io.quarkus.test.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import sh.ory.hydra.model.OAuth2TokenIntrospection

@QuarkusTest
@TestHTTPEndpoint(ErgochatController::class)
class ErgoChatControllerTest {

    @InjectMock
    @RestClient
    private lateinit var hydraClient: HydraClient

    @Inject
    private lateinit var properties: FafProperties

    @Test
    fun authenticateUnknownType() {
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "test:test", ip = "127.0.0.1")
        RestAssured.given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(false))
            .body("accountName", equalTo("test-user"))
            .body("error", notNullValue())
    }

    @Test
    fun authenticateStatic() {
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "static:banana", ip = "127.0.0.1")
        RestAssured.given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("accountName", equalTo("test-user"))
            .body("error", nullValue())
    }

    @Test
    fun authenticateStaticUnknownUser() {
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test", passphrase = "static:banana", ip = "127.0.0.1")
        RestAssured.given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(false))
            .body("accountName", equalTo("test"))
            .body("error", notNullValue())
    }

    @Test
    fun authenticateStaticBadPassword() {
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "static:ban", ip = "127.0.0.1")
        RestAssured.given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(false))
            .body("accountName", equalTo("test-user"))
            .body("error", notNullValue())
    }

    @Test
    fun authenticateOAuth() {
        whenever(hydraClient.introspectToken(any(), anyOrNull())).thenReturn(createActiveTokenForUsername("test-user"))
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "oauth:token", ip = "127.0.0.1")
        RestAssured.given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("accountName", equalTo("test-user"))
            .body("error", nullValue())
    }

    @Test
    fun authenticateOAuthInactive() {
        whenever(hydraClient.introspectToken(any(), anyOrNull())).thenReturn(createInactiveToken())
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "oauth:token", ip = "127.0.0.1")
        RestAssured.given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(false))
            .body("accountName", equalTo("test-user"))
            .body("error", notNullValue())
    }

    @Test
    fun authenticateOAuthUserMismatch() {
        whenever(hydraClient.introspectToken(any(), anyOrNull())).thenReturn(createActiveTokenForUsername("test"))
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "oauth:token", ip = "127.0.0.1")
        RestAssured.given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(false))
            .body("accountName", equalTo("test-user"))
            .body("error", notNullValue())
    }

    private fun createActiveTokenForUsername(username: String): OAuth2TokenIntrospection =
        OAuth2TokenIntrospection(active = true, ext = mapOf("username" to username))

    private fun createInactiveToken(): OAuth2TokenIntrospection =
        OAuth2TokenIntrospection(active = false)
}
