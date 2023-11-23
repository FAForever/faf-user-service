package com.faforever.userservice.web

import com.faforever.userservice.backend.hydra.HydraClient
import com.faforever.userservice.backend.hydra.RevokeRefreshTokensRequest
import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.OAuthScope
import com.faforever.userservice.web.util.FafRoleTest
import com.faforever.userservice.web.util.FafScopeTest
import io.quarkus.test.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.eclipse.microprofile.rest.client.inject.RestClient
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@QuarkusTest
@TestHTTPEndpoint(OAuthController::class)
class OAuthControllerTest {

    @InjectMock
    @RestClient
    private lateinit var hydraClient: HydraClient

    @BeforeEach
    fun setup() {
        val response = ResponseImpl()
        response.status = 204
        whenever(hydraClient.revokeRefreshTokens(any(), anyOrNull(), anyOrNull())).thenReturn(response)
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    fun cannotRevokeRefreshTokenWithoutClientOrAll() {
        RestAssured.given()
            .body(RevokeRefreshTokensRequest(subject = "1"))
            .contentType(ContentType.JSON)
            .post("/revokeTokens")
            .then()
            .statusCode(400)
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    fun canRevokeRefreshTokenWithAll() {
        RestAssured.given()
            .body(RevokeRefreshTokensRequest(subject = "1", all = true))
            .contentType(ContentType.JSON)
            .post("/revokeTokens")
            .then()
            .statusCode(204)
    }

    @Test
    @TestSecurity(authorizationEnabled = false)
    fun canRevokeRefreshTokenWithClient() {
        RestAssured.given()
            .body(RevokeRefreshTokensRequest(subject = "1", client = "test"))
            .contentType(ContentType.JSON)
            .post("/revokeTokens")
            .then()
            .statusCode(204)
    }

    @Test
    @TestSecurity(user = "test")
    @FafRoleTest([FafRole.ADMIN_ACCOUNT_BAN])
    @FafScopeTest([OAuthScope.ADMINISTRATIVE_ACTION])
    fun canRevokeRefreshTokenWithScopeAndRole() {
        RestAssured.given()
            .body(RevokeRefreshTokensRequest(subject = "1", all = true))
            .contentType(ContentType.JSON)
            .post("/revokeTokens")
            .then()
            .statusCode(204)
    }

    @Test
    @TestSecurity(user = "test")
    @FafScopeTest([OAuthScope.ADMINISTRATIVE_ACTION])
    fun cannotRevokeRefreshTokenWithOnlyScope() {
        RestAssured.post("revokeTokens").then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "test")
    @FafRoleTest([FafRole.ADMIN_ACCOUNT_BAN])
    fun cannotRevokeRefreshTokenWithOnlyRole() {
        RestAssured.post("revokeTokens").then().statusCode(403)
    }

    @Test
    @TestSecurity(user = "test")
    fun cannotRevokeRefreshTokenWithNoScopeAndNoRole() {
        RestAssured.post("revokeTokens").then().statusCode(403)
    }

    @Test
    fun cannotRevokeRefreshTokenUnAuthenticated() {
        RestAssured.post("revokeTokens").then().statusCode(401)
    }
}
