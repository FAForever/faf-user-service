package com.faforever.userservice.web.util

import io.quarkus.arc.Unremovable
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.test.security.TestSecurity
import io.quarkus.test.security.TestSecurityIdentityAugmentor
import jakarta.enterprise.context.ApplicationScoped
import jakarta.json.Json
import org.eclipse.microprofile.jwt.JsonWebToken

annotation class FafRoleTest(val value: Array<String>)
annotation class FafScopeTest(val value: Array<String>)

@ApplicationScoped
@Unremovable
class TestFafSecurityAugmentor : TestSecurityIdentityAugmentor {
    override fun augment(identity: SecurityIdentity, annotations: Array<out Annotation>): SecurityIdentity {
        val scopes = annotations.firstOrNull { it is FafScopeTest }?.let { it as FafScopeTest }?.value ?: arrayOf()
        val roles = annotations.firstOrNull { it is FafRoleTest }?.let { it as FafRoleTest }?.value ?: arrayOf()
        val username = annotations.firstOrNull { it is TestSecurity }?.let { it as TestSecurity }?.user ?: ""

        val builder = QuarkusSecurityIdentity.builder(identity)

        builder.setPrincipal(object : JsonWebToken {
            override fun getName(): String {
                return identity.principal.name
            }

            @Suppress("UNCHECKED_CAST")
            override fun <T> getClaim(claimName: String): T? {
                return when (claimName) {
                    "ext" -> {
                        mapOf(
                            "username" to Json.createValue(username),
                            "roles" to roles.map { value -> Json.createValue(value) }.toSet(),
                        ) as T
                    }

                    "scp" -> {
                        scopes.map { value -> Json.createValue(value) }.toSet() as T
                    }

                    else -> null
                }
            }

            override fun getClaimNames(): Set<String> {
                return setOf("ext")
            }
        })

        return builder.build()
    }
}
