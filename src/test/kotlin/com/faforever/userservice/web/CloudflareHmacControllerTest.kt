package com.faforever.userservice.web

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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@QuarkusTest
@TestHTTPEndpoint(CloudflareHmacController::class)
class CloudflareHmacControllerTest {

    @Inject
    private lateinit var fafProperties: FafProperties

    @Nested
    inner class LobbyEndpointTest {
        @Test
        @TestSecurity(authorizationEnabled = false)
        fun canRetrieveAccessUrl() {
            val lobby = fafProperties.lobby()
            RestAssured.given()
                .get("/lobby/access")
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
                .get("/lobby/access")
                .then()
                .statusCode(200)
        }

        @Test
        @TestSecurity(user = "test")
        @FafScopeTest([OAuthScope.LOBBY])
        fun cannotRetrieveAccessUrlWithOnlyScope() {
            RestAssured.get("/lobby/access").then().statusCode(403)
        }

        @Test
        @TestSecurity(user = "test")
        @FafRoleTest([FafRole.USER])
        fun cannotRetrieveAccessUrlWithOnlyRole() {
            RestAssured.get("/lobby/access").then().statusCode(403)
        }

        @Test
        @TestSecurity(user = "test")
        fun cannotRetrieveAccessUrlWithNoScopeAndNoRole() {
            RestAssured.get("/lobby/access").then().statusCode(403)
        }

        @Test
        fun cannotRetrieveAccessUrlUnAuthenticated() {
            RestAssured.get("/lobby/access").then().statusCode(401)
        }
    }

    @Nested
    inner class WebsocketEndpointTest {
        @Test
        @TestSecurity(authorizationEnabled = false)
        fun canRetrieveAccessUrl() {
            val replay = fafProperties.replay()
            RestAssured.given()
                .get("/replay/access")
                .then()
                .statusCode(200)
                .body("accessUrl", matchesRegex("${replay.accessUri()}\\?${replay.accessParam()}=\\d{10}-.{43,}"))
        }

        @Test
        @TestSecurity(user = "test")
        @FafRoleTest([FafRole.USER])
        @FafScopeTest([OAuthScope.LOBBY])
        fun canRetrieveAccessUrlWithScopeAndRole() {
            RestAssured.given()
                .get("/replay/access")
                .then()
                .statusCode(200)
        }

        @Test
        @TestSecurity(user = "test")
        @FafScopeTest([OAuthScope.LOBBY])
        fun cannotRetrieveAccessUrlWithOnlyScope() {
            RestAssured.get("/replay/access").then().statusCode(403)
        }

        @Test
        @TestSecurity(user = "test")
        @FafRoleTest([FafRole.USER])
        fun cannotRetrieveAccessUrlWithOnlyRole() {
            RestAssured.get("/replay/access").then().statusCode(403)
        }

        @Test
        @TestSecurity(user = "test")
        fun cannotRetrieveAccessUrlWithNoScopeAndNoRole() {
            RestAssured.get("/replay/access").then().statusCode(403)
        }

        @Test
        fun cannotRetrieveAccessUrlUnAuthenticated() {
            RestAssured.get("/replay/access").then().statusCode(401)
        }
    }

    @Nested
    inner class ChatEndpointTest {
        @Test
        @TestSecurity(authorizationEnabled = false)
        fun canRetrieveAccessUrl() {
            val chat = fafProperties.chat()
            RestAssured.given()
                .get("/chat/access")
                .then()
                .statusCode(200)
                .body("accessUrl", matchesRegex("${chat.accessUri()}\\?${chat.accessParam()}=\\d{10}-.{43,}"))
        }

        @Test
        @TestSecurity(user = "test")
        @FafRoleTest([FafRole.USER])
        @FafScopeTest([OAuthScope.LOBBY])
        fun canRetrieveAccessUrlWithScopeAndRole() {
            RestAssured.given()
                .get("/chat/access")
                .then()
                .statusCode(200)
        }

        @Test
        @TestSecurity(user = "test")
        @FafScopeTest([OAuthScope.LOBBY])
        fun cannotRetrieveAccessUrlWithOnlyScope() {
            RestAssured.get("/chat/access").then().statusCode(403)
        }

        @Test
        @TestSecurity(user = "test")
        @FafRoleTest([FafRole.USER])
        fun cannotRetrieveAccessUrlWithOnlyRole() {
            RestAssured.get("/chat/access").then().statusCode(403)
        }

        @Test
        @TestSecurity(user = "test")
        fun cannotRetrieveAccessUrlWithNoScopeAndNoRole() {
            RestAssured.get("/chat/access").then().statusCode(403)
        }

        @Test
        fun cannotRetrieveAccessUrlUnAuthenticated() {
            RestAssured.get("/chat/access").then().statusCode(401)
        }
    }
}
