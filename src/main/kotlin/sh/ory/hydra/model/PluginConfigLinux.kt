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
 * PluginConfigLinux plugin config linux
 * @param allowAllDevices allow all devices
 * @param capabilities capabilities
 * @param devices devices
 */

data class PluginConfigLinux(
    /* allow all devices */
    @field:JsonProperty("AllowAllDevices")
    val allowAllDevices: kotlin.Boolean,
    /* capabilities */
    @field:JsonProperty("Capabilities")
    val capabilities: kotlin.collections.List<kotlin.String>,
    /* devices */
    @field:JsonProperty("Devices")
    val devices: kotlin.collections.List<PluginDevice>
)
