package com.faforever.userservice.config

import io.micronaut.context.annotation.Context
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.thymeleaf.TemplateEngine

// FIXME: This is an eager loading approach increasing cold boot start time.
// Open topic in https://github.com/micronaut-projects/micronaut-core/issues/1180
@Singleton
@Context
class ThymeleafDialectLoader(private val templateEngine: TemplateEngine) {

    @PostConstruct
    fun setDialect() {
        templateEngine.addDialect(LayoutDialect())
    }
}