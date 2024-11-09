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
 * @param challenge Challenge is the identifier (\"logout challenge\") of the logout authentication request. It is used to identify the session.
 * @param client
 * @param requestUrl RequestURL is the original Logout URL requested.
 * @param rpInitiated RPInitiated is set to true if the request was initiated by a Relying Party (RP), also known as an OAuth 2.0 Client.
 * @param sid SessionID is the login session ID that was requested to log out.
 * @param subject Subject is the user for whom the logout was request.
 */

data class OAuth2LogoutRequest(

    /* Challenge is the identifier (\"logout challenge\") of the logout authentication request. It is used to identify the session. */
    @JsonProperty("challenge")
    val challenge: kotlin.String? = null,

    @JsonProperty("client")
    val client: OAuth2Client? = null,

    /* RequestURL is the original Logout URL requested. */
    @JsonProperty("request_url")
    val requestUrl: kotlin.String? = null,

    /* RPInitiated is set to true if the request was initiated by a Relying Party (RP), also known as an OAuth 2.0 Client. */
    @JsonProperty("rp_initiated")
    val rpInitiated: kotlin.Boolean? = null,

    /* SessionID is the login session ID that was requested to log out. */
    @JsonProperty("sid")
    val sid: kotlin.String? = null,

    /* Subject is the user for whom the logout was request. */
    @JsonProperty("subject")
    val subject: kotlin.String? = null,

)
