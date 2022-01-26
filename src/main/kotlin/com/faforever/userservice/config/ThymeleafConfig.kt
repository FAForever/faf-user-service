package com.faforever.userservice.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.thymeleaf.spring5.view.reactive.ThymeleafReactiveViewResolver

@Configuration
class ThymeleafConfig {
    @Autowired
    private lateinit var thymeleafViewResolver: ThymeleafReactiveViewResolver

    @Autowired
    private lateinit var fafProperties: FafProperties

    @Bean
    fun staticVariableProvider() {
        val map = mutableMapOf<String, Any>()
        map["environment"] = fafProperties.environment?.let { "[$it] " } ?: ""
        thymeleafViewResolver.staticVariables = map
    }
}
