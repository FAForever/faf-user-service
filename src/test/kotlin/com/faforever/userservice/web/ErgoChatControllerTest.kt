package com.faforever.userservice.web

import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.web.util.FafRoleTest
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured
import io.restassured.http.ContentType
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.matchesRegex
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test

@QuarkusTest
@TestHTTPEndpoint(ErgochatController::class)
class ErgoChatControllerTest {
    @Inject
    private lateinit var properties: FafProperties

    @Inject
    private lateinit var hmacService: HmacService

    @Test
    fun authenticateUnknownType() {
        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "test:test", ip = "127.0.0.1")
        RestAssured
            .given()
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
        RestAssured
            .given()
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
        RestAssured
            .given()
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
        RestAssured
            .given()
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
    @TestSecurity(user = "test-user")
    fun requestTokenWithoutRoleFails() {
        RestAssured
            .given()
            .get("/token")
            .then()
            .statusCode(403)
    }

    @Test
    @TestSecurity(user = "test-user")
    @FafRoleTest([FafRole.USER])
    fun requestAndAuthenticateIrcToken() {
        val token: String =
            RestAssured
                .given()
                .get("/token")
                .then()
                .statusCode(200)
                .body("value", matchesRegex("\\d{10}-.{43,}"))
                .extract()
                .body()
                .path("value")

        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "token:$token", ip = "127.0.0.1")

        RestAssured
            .given()
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
    @TestSecurity(user = "test-user")
    @FafRoleTest([FafRole.USER])
    fun requestAndAuthenticateIrcTokenExpired() {
        val token: String =
            RestAssured
                .given()
                .get("/token")
                .then()
                .statusCode(200)
                .body("value", matchesRegex("\\d{10}-.{43,}"))
                .extract()
                .body()
                .path("value")

        Thread.sleep(1000)

        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test-user", passphrase = "token:$token", ip = "127.0.0.1")

        RestAssured
            .given()
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
    @TestSecurity(user = "test-user")
    @FafRoleTest([FafRole.USER])
    fun requestAndAuthenticateIrcTokenUserMismatch() {
        val token: String =
            RestAssured
                .given()
                .get("/token")
                .then()
                .statusCode(200)
                .body("value", matchesRegex("\\d{10}-.{43,}"))
                .extract()
                .body()
                .path("value")

        val loginRequest =
            ErgochatController.LoginRequest(accountName = "test", passphrase = "token:$token", ip = "127.0.0.1")

        RestAssured
            .given()
            .body(loginRequest)
            .contentType(ContentType.JSON)
            .post("/login")
            .then()
            .statusCode(200)
            .body("success", equalTo(false))
            .body("accountName", equalTo("test"))
            .body("error", notNullValue())
    }
}
