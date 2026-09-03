package com.faforever.userservice.backend.account

import com.faforever.userservice.backend.security.HmacService
import com.faforever.userservice.config.FafProperties
import io.quarkus.rest.client.reactive.ClientExceptionMapper
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient

class WikiJsGraphqlException(message: String) : RuntimeException(message)

data class WikiJsGraphqlRequest(val query: String)

data class WikiJsGraphqlError(val message: String)

data class WikiJsSearchResponse(
    val data: SearchData?,
    val errors: List<WikiJsGraphqlError>?,
) {
    data class SearchData(val users: SearchUsers)
    data class SearchUsers(val search: List<WikiJsUser>)
}

data class WikiJsUser(val id: Int, val name: String)

data class WikiJsDeleteResponse(
    val data: DeleteData?,
    val errors: List<WikiJsGraphqlError>?,
) {
    data class DeleteData(val users: DeleteUsers)
    data class DeleteUsers(val delete: DeleteResult)
    data class DeleteResult(val responseResult: ResponseResult)
    data class ResponseResult(val succeeded: Boolean, val errorCode: Int?, val message: String?)
}

@ApplicationScoped
class WikiJsClientHeadersFactory(
    private val fafProperties: FafProperties,
    private val hmacService: HmacService,
) : ClientHeadersFactory {
    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>,
    ): MultivaluedMap<String, String> {
        val headers = MultivaluedHashMap<String, String>()
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer ${fafProperties.wikijs().token()}")

        fafProperties.jwt().hmac()?.let { hmacConfig ->
            headers.add("X-HMAC", hmacService.generateHmacToken(hmacConfig.message(), hmacConfig.secret()))
        }

        return headers
    }
}

@Path("/")
@ApplicationScoped
@RegisterRestClient(configKey = "wikijs-api")
@RegisterClientHeaders(WikiJsClientHeadersFactory::class)
interface WikiJsClient {

    companion object {
        @JvmStatic
        @ClientExceptionMapper
        @Blocking
        fun toException(response: Response): RuntimeException? =
            if (response.status !in 200..299) {
                WikiJsGraphqlException(
                    "WikiJS GraphQL request failed with status ${response.status}: " +
                        response.readEntity(String::class.java),
                )
            } else {
                null
            }
    }

    @POST
    fun search(request: WikiJsGraphqlRequest): WikiJsSearchResponse

    @POST
    fun delete(request: WikiJsGraphqlRequest): WikiJsDeleteResponse
}
