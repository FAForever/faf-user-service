package com.faforever.userservice.backend.security

import com.faforever.userservice.backend.domain.IpAddress
import io.quarkus.security.AuthenticationFailedException
import io.quarkus.security.identity.AuthenticationRequestContext
import io.quarkus.security.identity.IdentityProvider
import io.quarkus.security.identity.SecurityIdentity
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest
import io.quarkus.security.runtime.QuarkusSecurityIdentity
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class FafIdentityProvider(
    private val loginService: LoginService,
): IdentityProvider<UsernamePasswordAuthenticationRequest> {
    override fun getRequestType() = UsernamePasswordAuthenticationRequest::class.java

    override fun authenticate(
        request: UsernamePasswordAuthenticationRequest,
        context: AuthenticationRequestContext
    ): Uni<SecurityIdentity> =
        Uni.createFrom().item {
            val loginResult = loginService.login(
                usernameOrEmail = request.username,
                password = request.password.password.joinToString(separator = ""),
                ip = IpAddress("tbd"),
                requiresGameOwnership = false,
            )

            if(loginResult is LoginResult.SuccessfulLogin) {
                QuarkusSecurityIdentity.builder()
                    .setAnonymous(false)
                    .setPrincipal(FafPrincipal(userId = loginResult.userId, userName = loginResult.userName))
                    .addRole("user")
                    .build()
            } else {
                throw AuthenticationFailedException("Authentication failed")
            }
        }
}
