package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import io.quarkus.rest.client.reactive.ClientExceptionMapper
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

class NodebbUserNotFoundException : RuntimeException()

class NodebbAccountDeletionFailedException(message: String) : RuntimeException(message)

@ApplicationScoped
class NodebbClientHeadersFactory(
    private val fafProperties: FafProperties,
    private val hmacService: HmacService,
) : ClientHeadersFactory {
    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>,
    ): MultivaluedMap<String, String> {
        val headers = MultivaluedHashMap<String, String>()
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer ${fafProperties.nodebb().adminToken()}")

        fafProperties.jwt().hmac()?.let { hmacConfig ->
            headers.add("X-HMAC", hmacService.generateHmacToken(hmacConfig.message(), hmacConfig.secret()))
        }

        return headers
    }
}

@Path("/user")
@ApplicationScoped
@RegisterRestClient(configKey = "nodebb-read-api")
@RegisterClientHeaders(NodebbClientHeadersFactory::class)
interface NodebbReadClient {

    companion object {
        @JvmStatic
        @ClientExceptionMapper
        fun toException(response: Response): RuntimeException? =
            if (response.status == 404) NodebbUserNotFoundException() else null
    }

    @GET
    @Path("/username/{username}")
    fun getUserByUsername(@PathParam("username") username: String): NodebbUser
}

@Path("/users")
@ApplicationScoped
@RegisterRestClient(configKey = "nodebb-write-api")
@RegisterClientHeaders(NodebbClientHeadersFactory::class)
interface NodebbWriteClient {

    companion object {
        @JvmStatic
        @ClientExceptionMapper
        @Blocking
        fun toException(response: Response): RuntimeException? =
            if (response.status != 200) {
                NodebbAccountDeletionFailedException(
                    "NodeBB account deletion failed with status ${response.status}: " +
                        response.readEntity(String::class.java),
                )
            } else {
                null
            }
    }

    @DELETE
    @Path("/{uid}/account")
    fun deleteAccount(@PathParam("uid") uid: Int)
}

data class NodebbUser(val uid: Int)
