//package com.faforever.userservice
//
//import com.faforever.userservice.backend.security.OAuthCsrfValidationFilter
//import io.micronaut.context.annotation.Replaces
//import io.micronaut.core.async.publisher.Publishers
//import io.micronaut.http.HttpMethod
//import io.micronaut.http.HttpRequest
//import io.micronaut.http.HttpResponse
//import io.micronaut.http.HttpStatus
//import io.micronaut.http.MutableHttpResponse
//import io.micronaut.http.annotation.Filter
//import io.micronaut.http.filter.HttpServerFilter
//import io.micronaut.http.filter.ServerFilterChain
//import org.reactivestreams.Publisher
//
//const val CSRF_TOKEN = "CSRF_TOKEN"
//
//@Replaces(OAuthCsrfValidationFilter::class)
//@Filter(value = ["/oauth2/login", "/oauth2/consent"], methods = [HttpMethod.POST])
//class OAuthCsrfValidationTestFilter : HttpServerFilter {
//
//    override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
//        val requestParameter = request.parameters["_csrf"]
//
//        return if (requestParameter != CSRF_TOKEN) {
//            Publishers.just(HttpResponse.status<Any>(HttpStatus.FORBIDDEN))
//        } else {
//            chain.proceed(request)
//        }
//    }
//}
