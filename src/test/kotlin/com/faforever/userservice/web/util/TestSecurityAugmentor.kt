package com.faforever.userservice.web.util

import io.quarkus.arc.Unremovable
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.quarkus.test.security.TestSecurityIdentityAugmentor
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

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
