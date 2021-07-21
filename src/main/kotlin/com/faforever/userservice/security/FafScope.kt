package com.faforever.userservice.security

import com.faforever.userservice.security.OAuthScope.SCOPE_PREFIX
import org.springframework.security.core.GrantedAuthority

data class FafScope(val role: String) : GrantedAuthority {
    override fun getAuthority() = SCOPE_PREFIX + role
}

object OAuthScope {
    const val SCOPE_PREFIX = "SCOPE_"

    const val _PUBLIC_PROFILE = "public_profile"
    const val _WRITE_ACHIEVEMENTS = "write_achievements"
    const val _WRITE_EVENTS = "write_events"
    const val _UPLOAD_MAP = "upload_map"
    const val _UPLOAD_MOD = "upload_mod"
    const val _WRITE_ACCOUNT_DATA = "write_account_data"
    const val _EDIT_CLAN_DATA = "edit_clan_data"
    const val _VOTE = "vote"
    const val _READ_SENSIBLE_USERDATA = "read_sensible_userdata"
    const val _ADMINISTRATIVE_ACTION = "administrative_actions"
    const val _MANAGE_VAULT = "manage_vault"

    const val PUBLIC_PROFILE = SCOPE_PREFIX + _PUBLIC_PROFILE
    const val WRITE_ACHIEVEMENTS = SCOPE_PREFIX + _WRITE_ACHIEVEMENTS
    const val WRITE_EVENTS = SCOPE_PREFIX + _WRITE_EVENTS
    const val UPLOAD_MAP = SCOPE_PREFIX + _UPLOAD_MAP
    const val UPLOAD_MOD = SCOPE_PREFIX + _UPLOAD_MOD
    const val WRITE_ACCOUNT_DATA = SCOPE_PREFIX + _WRITE_ACCOUNT_DATA
    const val EDIT_CLAN_DATA = SCOPE_PREFIX + _EDIT_CLAN_DATA
    const val VOTE = SCOPE_PREFIX + _VOTE
    const val READ_SENSIBLE_USERDATA = SCOPE_PREFIX + _READ_SENSIBLE_USERDATA
    const val ADMINISTRATIVE_ACTION = SCOPE_PREFIX + _ADMINISTRATIVE_ACTION
    const val MANAGE_VAULT = SCOPE_PREFIX + _MANAGE_VAULT
}
