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
 * @param args
 * @param description description
 * @param documentation documentation
 * @param entrypoint entrypoint
 * @param env env
 * @param `interface`
 * @param ipcHost ipc host
 * @param linux
 * @param mounts mounts
 * @param network
 * @param pidHost pid host
 * @param propagatedMount propagated mount
 * @param workDir work dir
 * @param dockerVersion Docker Version used to create the plugin
 * @param user
 * @param rootfs
 */

data class PluginConfig(
    @field:JsonProperty("Args")
    val args: PluginConfigArgs,
    /* description */
    @field:JsonProperty("Description")
    val description: kotlin.String,
    /* documentation */
    @field:JsonProperty("Documentation")
    val documentation: kotlin.String,
    /* entrypoint */
    @field:JsonProperty("Entrypoint")
    val entrypoint: kotlin.collections.List<kotlin.String>,
    /* env */
    @field:JsonProperty("Env")
    val env: kotlin.collections.List<PluginEnv>,
    @field:JsonProperty("Interface")
    val `interface`: PluginConfigInterface,
    /* ipc host */
    @field:JsonProperty("IpcHost")
    val ipcHost: kotlin.Boolean,
    @field:JsonProperty("Linux")
    val linux: PluginConfigLinux,
    /* mounts */
    @field:JsonProperty("Mounts")
    val mounts: kotlin.collections.List<PluginMount>,
    @field:JsonProperty("Network")
    val network: PluginConfigNetwork,
    /* pid host */
    @field:JsonProperty("PidHost")
    val pidHost: kotlin.Boolean,
    /* propagated mount */
    @field:JsonProperty("PropagatedMount")
    val propagatedMount: kotlin.String,
    /* work dir */
    @field:JsonProperty("WorkDir")
    val workDir: kotlin.String,
    /* Docker Version used to create the plugin */
    @field:JsonProperty("DockerVersion")
    val dockerVersion: kotlin.String? = null,
    @field:JsonProperty("User")
    val user: PluginConfigUser? = null,
    @field:JsonProperty("rootfs")
    val rootfs: PluginConfigRootfs? = null
)
