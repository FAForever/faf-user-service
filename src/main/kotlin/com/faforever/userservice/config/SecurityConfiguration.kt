package com.faforever.userservice.config

import com.faforever.userservice.security.FafJwtAuthenticationConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
// Import required as of https://youtrack.jetbrains.com/issue/KT-43578
import org.springframework.security.config.web.server.invoke
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@Configuration
class SecurityConfiguration {

    @Bean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity,
        fafJwtAuthenticationConverter: FafJwtAuthenticationConverter,
    ): SecurityWebFilterChain =
        http {
            authorizeExchange {
                authorize("/", permitAll)
                authorize("/robots.txt", permitAll)
                authorize("/favicon.ico", permitAll)
                authorize("/login", permitAll)
                authorize("/consent", permitAll)
                authorize("/css/**", permitAll)
                authorize("/js/**", permitAll)
                authorize("/**", authenticated)
            }
            oauth2ResourceServer {
                jwt {
                    jwtAuthenticationConverter = ReactiveJwtAuthenticationConverter().apply {
                        setJwtGrantedAuthoritiesConverter(fafJwtAuthenticationConverter)
                    }
                }
            }
            csrf { }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
        }
}
