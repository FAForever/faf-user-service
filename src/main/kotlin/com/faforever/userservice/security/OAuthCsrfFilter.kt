package com.faforever.userservice.security

import com.faforever.userservice.config.FafProperties
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
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
import org.slf4j.LoggerFactory
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

@Filter(value = ["/oauth2/login", "/oauth2/consent"], methods = [HttpMethod.POST])
@Replaces(OAuthCsrfValidationFilter::class)
@Requires(bean = FafProperties::class, beanProperty = "disableCsrf", value = "true")
class OAuthCsrfValidationDebugFilter : HttpServerFilter {
    companion object {
        private val LOG = LoggerFactory.getLogger(OAuthCsrfValidationDebugFilter::class.java)
    }

    init {
        LOG.warn("CSRF validation is disabled. Do not use this in production!")
    }

    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
        val requestParameter = request.parameters["_csrf"]
        val cookieValue = request.cookies.findCookie("_csrf").map { obj: Cookie -> obj.value }.orElse(null)

        if (cookieValue == null || cookieValue != requestParameter) {
            LOG.warn("CSRF check failed (but is disabled). cookieValue: $cookieValue, requestParameter: $requestParameter")
        } else {
            LOG.debug("CSRF check passed. cookieValue: $cookieValue, requestParameter: $requestParameter")
        }

        return chain.proceed(request)
    }
}
