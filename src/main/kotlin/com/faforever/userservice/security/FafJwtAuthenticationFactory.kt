package com.faforever.userservice.security

import com.nimbusds.jwt.JWT
import io.micronaut.context.annotation.Replaces
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.jwt.validator.DefaultJwtAuthenticationFactory
import io.micronaut.security.token.jwt.validator.JwtAuthenticationFactory
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import java.text.ParseException
import java.util.*

@Singleton
@Replaces(DefaultJwtAuthenticationFactory::class)
class FafJwtAuthenticationFactory : JwtAuthenticationFactory {
    companion object {
        private val LOG = LoggerFactory.getLogger(FafJwtAuthenticationFactory::class.java)
    }

    override fun createAuthentication(token: JWT?): Optional<Authentication> {
        // TODO: Support machine-to-machine JWTs (client credentials flow)
        // This raises some questions about which properties of FafUserAuthentication are even available
        return try {
            val claimSet = token?.jwtClaimsSet ?: return Optional.empty()
            val attributes = claimSet.claims

            @Suppress("UNCHECKED_CAST")
            val extensions = attributes["ext"] as Map<String, Any>

            @Suppress("UNCHECKED_CAST")
            Optional.of(
                FafUserAuthentication(
                    (attributes["sub"] as String).toInt(),
                    extensions["username"] as String,
                    attributes["scp"] as List<String>,
                    extensions["roles"] as List<String>,
                    attributes
                )
            )
        } catch (e: ParseException) {
            LOG.error("ParseException creating authentication", e)
            Optional.empty()
        }
    }
}
