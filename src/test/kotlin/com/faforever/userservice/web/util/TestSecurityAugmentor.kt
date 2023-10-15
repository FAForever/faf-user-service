package com.faforever.userservice.web.util

import io.quarkus.arc.Unremovable
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.TestSecurityIdentityAugmentor
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.Json
import org.eclipse.microprofile.jwt.JsonWebToken

annotation class FafRoleTest(val value: Array<String>)
annotation class FafScopeTest(val value: Array<String>)

@ApplicationScoped
@Unremovable
class TestFafSecurityAugmentor : TestSecurityIdentityAugmentor {
    override fun augment(identity: SecurityIdentity, annotations: Array<out Annotation>): SecurityIdentity {
        val scopes = annotations.firstOrNull { it is FafScopeTest }?.let { it as FafScopeTest }?.value
        val roles = annotations.firstOrNull { it is FafRoleTest }?.let { it as FafRoleTest }?.value
        val username = annotations.firstOrNull { it is TestSecurity }?.let { it as TestSecurity }?.user

        val builder = QuarkusSecurityIdentity.builder(identity)
        builder.addPermissionChecker { requiredPermission ->
            val hasRole = roles?.contains(requiredPermission.name) == true
            val hasScopes = requiredPermission.actions.split(",").all { scopes?.contains(it) == true }
            Uni.createFrom().item(hasRole && hasScopes)
        }

        roles?.let { builder.addRoles(setOf(*it)) }

        builder.setPrincipal(object : JsonWebToken {
            override fun getName(): String {
                return identity.principal.name
            }

            @Suppress("UNCHECKED_CAST")
            override fun <T> getClaim(claimName: String): T? {
                if (claimName == "ext") {
                    return mapOf("username" to Json.createValue(username)) as T
                }
                return null
            }

            override fun getClaimNames(): Set<String> {
                return setOf("ext")
            }
        })

        return builder.build()
    }
}
