package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.domain.User
import io.quarkus.security.identity.SecurityIdentity
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Singleton
class CurrentUserService(
    private val securityIdentity: SecurityIdentity,
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(CurrentUserService::class.java)
    }

    // TODO: Implement Vaadin auth mechanism and reload from database or something
    fun requireUser(): User = User(
        5,
        "zep",
        "thisshouldnotbehere",
        email = "iam@faforever.com",
        null,
    )

    fun invalidate() {
        log.debug("Invalidating current user")
        // TODO: Invalidate cache
    }
}
