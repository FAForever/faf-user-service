package com.faforever.userservice

import com.faforever.userservice.security.FafUserAuthentication
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.filters.AuthenticationFetcher
import io.micronaut.security.token.TokenAuthenticationFetcher
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.util.*

@Singleton
@Replaces(TokenAuthenticationFetcher::class)
class TestAuthenticationFetcher : AuthenticationFetcher {
    val authenticationQueue: Queue<Authentication> = ArrayDeque()

    fun setNextAuthentications(vararg authentication: FafUserAuthentication) {
        authenticationQueue.clear()
        authentication.forEach { authenticationQueue.add(it) }
    }

    override fun fetchAuthentication(request: HttpRequest<*>?): Publisher<Authentication> =
        when (authenticationQueue.size) {
            0 -> Mono.empty()
            1 -> authenticationQueue.peek().toMono()
            else -> authenticationQueue.poll().toMono()
        }
}
