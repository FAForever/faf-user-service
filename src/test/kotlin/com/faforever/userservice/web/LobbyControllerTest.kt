package com.faforever.userservice.web

import com.faforever.userservice.backend.cloudflare.CloudflareService
import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.OAuthScope
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.web.util.FafRoleTest
import com.faforever.userservice.web.util.FafScopeTest
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured
import jakarta.inject.Inject
import org.hamcrest.Matchers.matchesRegex
import org.junit.jupiter.api.Test

@QuarkusTest
@TestHTTPEndpoint(LobbyController::class)
class LobbyControllerTest {

    @Inject
    private lateinit var fafProperties: FafProperties

    @Inject
    private lateinit var cloudflareService: CloudflareService

    @Test
    @TestSecurity(authorizationEnabled = false)
    fun canRetrieveAccessUrl() {
        val lobby = fafProperties.lobby()
        RestAssured.given()
            .get("/access")
            .then()
            .statusCode(200)
            .body("accessUrl", matchesRegex("${lobby.accessUri()}\\?${lobby.accessParam()}=\\d{10}-.{43,}"))
    }

    @Test
    @TestSecurity(user = "test")
    @FafRoleTest([FafRole.USER])
    @FafScopeTest([OAuthScope.LOBBY])
    fun canRetrieveAccessUrlWithScopeAndRole() {
        RestAssured.given()
            .get("/access")
            .then()
            .statusCode(200)
    }

    @Test
    @TestSecurity(user = "test")
    @FafScopeTest([OAuthScope.LOBBY])
    fun cannotRetrieveAccessUrlWithOnlyScope() {
        RestAssured.get("/access").then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "test")
    @FafRoleTest([FafRole.USER])
    fun cannotRetrieveAccessUrlWithOnlyRole() {
        RestAssured.get("/access").then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "test")
    fun cannotRetrieveAccessUrlWithNoScopeAndNoRole() {
        RestAssured.get("/access").then().statusCode(403)
    }

    @Test
    fun cannotRetrieveAccessUrlUnAuthenticated() {
        RestAssured.get("/access").then().statusCode(401)
    }
}
