package com.faforever.usermanagement.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
// Import required as of https://youtrack.jetbrains.com/issue/KT-43578
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.password.NoOpPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@EnableWebFluxSecurity
@Configuration
class SecurityConfiguration {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http {
            authorizeExchange {
                authorize("/", permitAll)
                authorize("/robots.txt", permitAll)
                authorize("/favicon.ico", permitAll)
                authorize("/login", permitAll)
                authorize("/consent", permitAll)
                authorize("/css/**", permitAll)
                authorize("/**", authenticated)
            }
            csrf { }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
        }

    @Bean
    fun userDetailsService(passwordEncoder: PasswordEncoder): MapReactiveUserDetailsService {
        val user: UserDetails = User
            .withUsername("user")
            .password(passwordEncoder.encode("password"))
            .roles("USER")
            .build()
        return MapReactiveUserDetailsService(user)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = NoOpPasswordEncoder.getInstance()
}
