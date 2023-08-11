package com.faforever.userservice

import com.faforever.userservice.backend.hydra.HydraClient
import com.faforever.userservice.backend.hydra.RevokeRefreshTokensRequest
import com.faforever.userservice.backend.security.FafRole
import com.faforever.userservice.backend.security.OAuthScope
import com.faforever.userservice.web.OAuthController
import io.quarkus.arc.Unremovable
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.test.InjectMock
import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.TestSecurityIdentityAugmentor
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
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

annotation class FafRoleTest(val value: Array<String>)
annotation class FafScopeTest(val value: Array<String>)

@ApplicationScoped
@Unremovable
class TestFafSecurityAugmentor : TestSecurityIdentityAugmentor {
    override fun augment(identity: SecurityIdentity?, annotations: Array<out Annotation>?): SecurityIdentity {
        val scopes = annotations?.firstOrNull { it is FafScopeTest }?.let { it as FafScopeTest }?.value
        val roles = annotations?.firstOrNull { it is FafRoleTest }?.let { it as FafRoleTest }?.value

        val builder = QuarkusSecurityIdentity.builder(identity)
        builder.addPermissionChecker { requiredPermission ->
            val hasRole = roles?.contains(requiredPermission.name) == true
            val hasScopes = requiredPermission.actions.split(",").all { scopes?.contains(it) == true }
            Uni.createFrom().item(hasRole && hasScopes)
        }
        return builder.build()
    }
}
