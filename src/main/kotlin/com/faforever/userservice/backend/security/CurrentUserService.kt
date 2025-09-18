package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.domain.User
import com.faforever.userservice.backend.domain.UserRepository
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.runtime.AnonymousIdentityProvider
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.NotAuthorizedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ApplicationScoped
class CurrentUserService(
    private val securityIdentity: SecurityIdentity,
    private val userRepository: UserRepository,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(CurrentUserService::class.java)
    }

    fun requireUser(): User = when (securityIdentity.principal) {
        is AnonymousIdentityProvider -> throw NotAuthorizedException("User not logged in")
        is FafPrincipal -> {
            val username = securityIdentity.principal.name
            log.debug("Fetching current user from database for: {}", username)

            userRepository.findByUsernameOrEmail(username)
                ?: throw NotAuthorizedException("User not found in database: $username")
        }

        else -> throw NotAuthorizedException("Unknown principal: ${securityIdentity.principal}")
    }


    fun invalidate() {
        log.debug("Invalidating current user")
        // TODO: Invalidate cache
    }
}
