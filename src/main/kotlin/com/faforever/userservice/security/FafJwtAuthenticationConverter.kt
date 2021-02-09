package com.faforever.userservice.security

import com.nimbusds.jose.shaded.json.JSONArray
import com.nimbusds.jose.shaded.json.JSONObject
import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux

data class FafAuthority(val role: String) : GrantedAuthority {
    override fun getAuthority() = "ROLE_$role"
}

/**
 * Extract the FAF roles that we put into the ext->roles array of the access token.
 */
@Component
class FafJwtAuthenticationConverter : Converter<Jwt, Flux<GrantedAuthority>> {
    override fun convert(jwt: Jwt): Flux<GrantedAuthority> {
        val ext: JSONObject? = jwt.claims["ext"] as? JSONObject
        val roles = ext?.get("roles") as? JSONArray
        val authorities = roles?.map { FafAuthority(it.toString()) } ?: listOf()

        return authorities.toFlux()
    }
}
