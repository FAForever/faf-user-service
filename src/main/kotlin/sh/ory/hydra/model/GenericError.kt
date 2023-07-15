/**
* ORY Hydra
* Welcome to the ORY Hydra HTTP API documentation. You will find documentation for all HTTP APIs here.
*
* The version of the OpenAPI document: latest
*
*
* NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
* https://openapi-generator.tech
* Do not edit the class manually.
*/
package sh.ory.hydra.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.quarkus.runtime.annotations.RegisterForReflection

/**
 * Error responses are sent when an error (e.g. unauthorized, bad request, ...) occurred.
 * @param error Name is the error name.
 * @param debug Debug contains debug information. This is usually not available and has to be enabled.
 * @param errorDescription Description contains further information on the nature of the error.
 * @param statusCode Code represents the error status code (404, 403, 401, ...).
 */

@RegisterForReflection
data class GenericError(
    /* Name is the error name. */
    @JsonProperty("error")
    val error: kotlin.String,
    /* Debug contains debug information. This is usually not available and has to be enabled. */
     @JsonProperty("debug")
     val debug: kotlin.String? = null,
    // /* Description contains further information on the nature of the error. */
     @JsonProperty("error_description")
     val errorDescription: kotlin.String? = null,
    // /* Code represents the error status code (404, 403, 401, ...). */
     @JsonProperty("status_code")
     val statusCode: kotlin.Long? = null
)
