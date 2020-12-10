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

/**
 *
 * @param error The error should follow the OAuth2 error format (e.g. `invalid_request`, `login_required`).  Defaults to `request_denied`.
 * @param errorDebug Debug contains information to help resolve the problem as a developer. Usually not exposed to the public but only in the server logs.
 * @param errorDescription Description of the error in a human readable format.
 * @param errorHint Hint to help resolve the error.
 * @param statusCode Represents the HTTP status code of the error (e.g. 401 or 403)  Defaults to 400
 */

data class RejectRequest(
    /* The error should follow the OAuth2 error format (e.g. `invalid_request`, `login_required`).  Defaults to `request_denied`. */
    @field:JsonProperty("error")
    val error: kotlin.String? = null,
    /* Debug contains information to help resolve the problem as a developer. Usually not exposed to the public but only in the server logs. */
    @field:JsonProperty("error_debug")
    val errorDebug: kotlin.String? = null,
    /* Description of the error in a human readable format. */
    @field:JsonProperty("error_description")
    val errorDescription: kotlin.String? = null,
    /* Hint to help resolve the error. */
    @field:JsonProperty("error_hint")
    val errorHint: kotlin.String? = null,
    /* Represents the HTTP status code of the error (e.g. 401 or 403)  Defaults to 400 */
    @field:JsonProperty("status_code")
    val statusCode: kotlin.Long? = null
)
