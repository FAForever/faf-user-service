package com.faforever.userservice.backend.security

/**
 * Contains pre-defined FAF scopes
 */
object OAuthScope {
    // OIDC scopes
    const val EMAIL = "email"
    const val PROFILE = "profile"

    // FAF scopes
    const val PUBLIC_PROFILE = "public_profile"
    const val WRITE_ACHIEVEMENTS = "write_achievements"
    const val WRITE_EVENTS = "write_events"
    const val UPLOAD_MAP = "upload_map"
    const val UPLOAD_MOD = "upload_mod"
    const val WRITE_ACCOUNT_DATA = "write_account_data"
    const val EDIT_CLAN_DATA = "edit_clan_data"
    const val VOTE = "vote"
    const val LOBBY = "lobby"
    const val READ_SENSIBLE_USERDATA = "read_sensible_userdata"
    const val ADMINISTRATIVE_ACTION = "administrative_actions"
    const val MANAGE_VAULT = "manage_vault"

    fun canShowEmail(scopes: List<String>?) =
        if (scopes == null) {
            false
        } else scopes.contains(EMAIL) || scopes.contains(PROFILE) || scopes.contains(PUBLIC_PROFILE)
}
