/**
* ORY Hydra
* Welcome to the ORY Hydra HTTP API documentation. You will find documentation for all HTTP APIs here.
*
* The version of the OpenAPI document: latest
* *
* NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
* https://openapi-generator.tech
* Do not edit the class manually.
*/
package sh.ory.hydra.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * * @param redirectTo Original request URL to which you should redirect the user if request was already handled.
 */

data class RequestWasHandledResponse(
    /* Original request URL to which you should redirect the user if request was already handled. */
    @JsonProperty("redirect_to")
    val redirectTo: kotlin.String,
)
