package com.faforever.userservice.security

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.cookie.Cookie
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.netty.cookies.NettyCookie
import org.reactivestreams.Publisher
import reactor.kotlin.core.publisher.toMono
import java.time.Instant
import java.util.*

@Filter(value = ["/oauth2/login", "/oauth2/consent"], methods = [HttpMethod.GET])
class OAuthCsrfInjectionFilter : HttpServerFilter {

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val csrfToken = "${UUID.randomUUID()}@${Instant.now()}"

        request.mutate().apply {
            attributes.put("_csrf", csrfToken)
        }

        return chain.proceed(request)
            .toMono()
            .map {
                it.cookie(NettyCookie("_csrf", csrfToken))
            }
    }
}

@Filter(value = ["/oauth2/login", "/oauth2/consent"], methods = [HttpMethod.POST])
class OAuthCsrfValidationFilter : HttpServerFilter {

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val requestParameter = request.parameters["_csrf"]
        val cookieValue = request.cookies.findCookie("_csrf").map { obj: Cookie -> obj.value }.orElse(null)

        return if (cookieValue == null || cookieValue != requestParameter) {
            Publishers.just(HttpResponse.status<Any>(HttpStatus.FORBIDDEN))
        } else {
            chain.proceed(request)
        }
    }
}
