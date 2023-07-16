package com.faforever.userservice.config

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.i18n.ResourceBundleMessageSource
import java.util.*

@Factory
open class I18nConfig {
    @Bean
    fun messageSource() = ResourceBundleMessageSource("i18n.messages", Locale.ENGLISH)
}
