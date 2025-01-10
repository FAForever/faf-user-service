/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package sh.ory.hydra.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 *
 *
 * @param message Error message  The error's message.
 * @param code The status code
 * @param debug Debug information  This field is often not exposed to protect against leaking sensitive information.
 * @param details Further error details
 * @param id The error ID  Useful when trying to identify various errors in application logic.
 * @param reason A human-readable reason for the error
 * @param request The request ID  The request ID is often exposed internally in order to trace errors across service architectures. This is often a UUID.
 * @param status The status description
 */

data class GenericError(

    /* Error message  The error's message. */
    @JsonProperty("message")
    val message: kotlin.String,

    /* The status code */
    @JsonProperty("code")
    val code: kotlin.Long? = null,

    /* Debug information  This field is often not exposed to protect against leaking sensitive information. */
    @JsonProperty("debug")
    val debug: kotlin.String? = null,

    /* Further error details */
    @JsonProperty("details")
    val details: kotlin.Any? = null,

    /* The error ID  Useful when trying to identify various errors in application logic. */
    @JsonProperty("id")
    val id: kotlin.String? = null,

    /* A human-readable reason for the error */
    @JsonProperty("reason")
    val reason: kotlin.String? = null,

    /* The request ID  The request ID is often exposed internally in order to trace errors across service architectures. This is often a UUID. */
    @JsonProperty("request")
    val request: kotlin.String? = null,

    /* The status description */
    @JsonProperty("status")
    val status: kotlin.String? = null,

)
