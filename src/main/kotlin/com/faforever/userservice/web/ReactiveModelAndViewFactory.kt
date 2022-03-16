package com.faforever.userservice.web

import com.faforever.userservice.config.FafProperties
import io.micronaut.http.HttpResponse
import io.micronaut.http.uri.UriBuilder
import io.micronaut.runtime.http.scope.RequestScope
import io.micronaut.views.ModelAndView
import io.opentracing.Tracer
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

private const val TRACE_ID = "traceId"

@RequestScope
class ReactiveModelAndViewFactory(
    properties: FafProperties,
    private val tracer: Tracer
) {
    private val variables = mutableMapOf<String, Any?>()

    init {
        variables["environment"] = properties.environment?.let { "[$it]" } ?: ""
    }

    fun with(key: String, value: Any?): ReactiveModelAndViewFactory {
        variables[key] = value
        return this
    }

    fun build(viewName: String): Mono<HttpResponseWithModelView> {
        return HttpResponse.ok(
            ModelAndView(viewName, variables as Map<String, Any?>)
        ).toMono()
    }

    fun buildError(errorViewName: String): Mono<HttpResponseWithModelView> {
        if (variables[TRACE_ID] == null) {
            variables[TRACE_ID] = traceId
        }
        return HttpResponse.ok(
            ModelAndView(errorViewName, variables as Map<String, Any?>)
        ).toMono()
    }

    private val traceId get() = tracer.activeSpan().context().toTraceId()

    fun redirectError(url: String): HttpResponseWithModelView {
        val destination = UriBuilder.of(url)
            .queryParam(TRACE_ID, traceId)
            .build()

        return HttpResponse.redirect(destination)
    }
}