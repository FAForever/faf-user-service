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
 *
 * @param challenge ID is the identifier (\"authorization challenge\") of the consent authorization request. It is used to identify the session.
 * @param acr ACR represents the Authentication AuthorizationContext Class Reference value for this authentication session. You can use it to express that, for example, a user authenticated using two factor authentication.
 * @param client
 * @param context
 * @param loginChallenge LoginChallenge is the login challenge this consent challenge belongs to. It can be used to associate a login and consent request in the login & consent app.
 * @param loginSessionId LoginSessionID is the login session ID. If the user-agent reuses a login session (via cookie / remember flag) this ID will remain the same. If the user-agent did not have an existing authentication session (e.g. remember is false) this will be a new random value. This value is used as the \"sid\" parameter in the ID Token and in OIDC Front-/Back- channel logout. It's value can generally be used to associate consecutive login requests by a certain user.
 * @param oidcContext
 * @param requestUrl RequestURL is the original OAuth 2.0 Authorization URL requested by the OAuth 2.0 client. It is the URL which initiates the OAuth 2.0 Authorization Code or OAuth 2.0 Implicit flow. This URL is typically not needed, but might come in handy if you want to deal with additional request parameters.
 * @param requestedAccessTokenAudience
 * @param requestedScope
 * @param skip Skip, if true, implies that the client has requested the same scopes from the same user previously. If true, you must not ask the user to grant the requested scopes. You must however either allow or deny the consent request using the usual API call.
 * @param subject Subject is the user ID of the end-user that authenticated. Now, that end user needs to grant or deny the scope requested by the OAuth 2.0 client.
 */

@RegisterForReflection
data class ConsentRequest(
    /* ID is the identifier (\"authorization challenge\") of the consent authorization request. It is used to identify the session. */
    @field:JsonProperty("challenge")
    val challenge: kotlin.String,
    /* ACR represents the Authentication AuthorizationContext Class Reference value for this authentication session. You can use it to express that, for example, a user authenticated using two factor authentication. */
    @field:JsonProperty("acr")
    val acr: kotlin.String? = null,
    @field:JsonProperty("client")
    val client: OAuth2Client? = null,
    @field:JsonProperty("context")
    val context: kotlin.Any? = null,
    /* LoginChallenge is the login challenge this consent challenge belongs to. It can be used to associate a login and consent request in the login & consent app. */
    @field:JsonProperty("login_challenge")
    val loginChallenge: kotlin.String? = null,
    /* LoginSessionID is the login session ID. If the user-agent reuses a login session (via cookie / remember flag) this ID will remain the same. If the user-agent did not have an existing authentication session (e.g. remember is false) this will be a new random value. This value is used as the \"sid\" parameter in the ID Token and in OIDC Front-/Back- channel logout. It's value can generally be used to associate consecutive login requests by a certain user. */
    @field:JsonProperty("login_session_id")
    val loginSessionId: kotlin.String? = null,
    @field:JsonProperty("oidc_context")
    val oidcContext: OpenIDConnectContext? = null,
    /* RequestURL is the original OAuth 2.0 Authorization URL requested by the OAuth 2.0 client. It is the URL which initiates the OAuth 2.0 Authorization Code or OAuth 2.0 Implicit flow. This URL is typically not needed, but might come in handy if you want to deal with additional request parameters. */
    @field:JsonProperty("request_url")
    val requestUrl: kotlin.String? = null,
    @field:JsonProperty("requested_access_token_audience")
    val requestedAccessTokenAudience: kotlin.collections.List<kotlin.String>? = null,
    @field:JsonProperty("requested_scope")
    val requestedScope: kotlin.collections.List<kotlin.String>? = null,
    /* Skip, if true, implies that the client has requested the same scopes from the same user previously. If true, you must not ask the user to grant the requested scopes. You must however either allow or deny the consent request using the usual API call. */
    @field:JsonProperty("skip")
    val skip: kotlin.Boolean? = null,
    /* Subject is the user ID of the end-user that authenticated. Now, that end user needs to grant or deny the scope requested by the OAuth 2.0 client. */
    @field:JsonProperty("subject")
    val subject: kotlin.String? = null,
)
