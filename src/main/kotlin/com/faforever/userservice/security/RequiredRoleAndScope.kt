package com.faforever.userservice.security

import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.micronaut.security.rules.SecurityRuleResult
import io.micronaut.web.router.MethodBasedRouteMatch
import io.micronaut.web.router.RouteMatch
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Target(AnnotationTarget.FUNCTION)
@Retention
annotation class RequiredRoleAndScope(
    val scope: String,
    val role: String,
)

@Singleton
class RequiredRoleAndScopeRule : SecurityRule {
    companion object {
        val LOG: Logger = LoggerFactory.getLogger(RequiredRoleAndScopeRule::class.java)
    }

    override fun check(
        request: HttpRequest<*>,
        routeMatch: RouteMatch<*>?,
        authentication: Authentication?
    ): Publisher<SecurityRuleResult> {
        if (authentication == null) {
            return SecurityRuleResult.REJECTED.toMono()
        }

        return when (routeMatch) {
            is MethodBasedRouteMatch<*, *> -> {
                if (authentication !is FafUserAuthentication) {
                    return Mono.error(IllegalStateException("authentication is of wrong type"))
                }

                val annotation = routeMatch.getAnnotation(RequiredRoleAndScope::class.java)
                if (annotation != null) {
                    // the annotation values are not nullable
                    val scope = annotation.stringValue("scope").get()
                    val role = annotation.stringValue("role").get()

                    val result = if (!authentication.scopes.contains(scope)) {
                        LOG.debug("Scope {} is not available", scope)
                        SecurityRuleResult.REJECTED
                    } else if (!authentication.roles.contains(role)) {
                        LOG.debug("Role {} is not available", scope)
                        SecurityRuleResult.REJECTED
                    } else {
                        LOG.debug("Scope {} and role {} are allowed", scope, role)
                        SecurityRuleResult.ALLOWED
                    }

                    result.toMono()
                } else {
                    SecurityRuleResult.UNKNOWN.toMono()
                }
            }
            else -> SecurityRuleResult.UNKNOWN.toMono()
        }
    }
}
