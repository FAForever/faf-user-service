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
 * PluginMount plugin mount
 * @param description description
 * @param destination destination
 * @param name name
 * @param options options
 * @param settable settable
 * @param source source
 * @param type type
 */

data class PluginMount(
    /* description */
    @field:JsonProperty("Description")
    val description: kotlin.String,
    /* destination */
    @field:JsonProperty("Destination")
    val destination: kotlin.String,
    /* name */
    @field:JsonProperty("Name")
    val name: kotlin.String,
    /* options */
    @field:JsonProperty("Options")
    val options: kotlin.collections.List<kotlin.String>,
    /* settable */
    @field:JsonProperty("Settable")
    val settable: kotlin.collections.List<kotlin.String>,
    /* source */
    @field:JsonProperty("Source")
    val source: kotlin.String,
    /* type */
    @field:JsonProperty("Type")
    val type: kotlin.String
)
