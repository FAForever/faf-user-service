package com.faforever.userservice

import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.function.Supplier

// Workaround as described in https://github.com/spring-projects/spring-framework/issues/20606#issuecomment-712060119
internal class MutatorFilter : WebFilter {

    override fun filter(exchange: ServerWebExchange, webFilterChain: WebFilterChain): Mono<Void> {
        val context = exchange.getAttribute<Supplier<Mono<SecurityContext>>>(ATTRIBUTE_NAME)
        if (context != null) {
            exchange.attributes.remove(ATTRIBUTE_NAME)
            return webFilterChain.filter(exchange)
                .subscriberContext(ReactiveSecurityContextHolder.withSecurityContext(context.get()))
        }
        return webFilterChain.filter(exchange)
    }

    companion object {
        const val ATTRIBUTE_NAME = "context"
    }
}
