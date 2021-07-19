package com.faforever.userservice.security

import com.nimbusds.jose.shaded.json.JSONArray
import com.nimbusds.jose.shaded.json.JSONObject
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux

/**
 * Extract the FAF roles that we put into the ext->roles array of the access token and the scopes.
 */
@Component
class FafJwtAuthenticationConverter : Converter<Jwt, Flux<GrantedAuthority>> {
    override fun convert(jwt: Jwt): Flux<GrantedAuthority> {
        val ext = jwt.claims["ext"] as? JSONObject
        val scopes = jwt.claims["scp"] as JSONArray
        val roles = ext?.get("roles") as? JSONArray
        val authorities = scopes.map { FafScope(it.toString()) } + (roles?.map { FafRole(it.toString()) } ?: listOf())

        return authorities.toFlux()
    }
}
