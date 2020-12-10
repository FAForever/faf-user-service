package com.faforever.userservice.security

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class ScopeService {
    fun hasScope(authentication: Authentication, scope: String): Boolean {
        return authentication.authorities.contains(FafScope(scope))
    }
}
