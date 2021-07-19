package com.faforever.userservice.security

import com.faforever.userservice.security.OAuthRole.ROLE_PREFIX
import org.springframework.security.core.GrantedAuthority

data class FafRole(val role: String) : GrantedAuthority {
    override fun getAuthority() = ROLE_PREFIX + role
}

object OAuthRole {
    const val ROLE_PREFIX = "ROLE_"

    const val READ_AUDIT_LOG = "READ_AUDIT_LOG"
    const val READ_TEAMKILL_REPORT = "READ_TEAMKILL_REPORT"
    const val READ_ACCOUNT_PRIVATE_DETAILS = "READ_ACCOUNT_PRIVATE_DETAILS"
    const val ADMIN_ACCOUNT_NOTE = "ADMIN_ACCOUNT_NOTE"
    const val ADMIN_ACCOUNT_NAME_CHANGE = "ADMIN_ACCOUNT_NAME_CHANGE"
    const val ADMIN_MODERATION_REPORT = "ADMIN_MODERATION_REPORT"
    const val ADMIN_ACCOUNT_BAN = "ADMIN_ACCOUNT_BAN"
    const val ADMIN_CLAN = "ADMIN_CLAN"
    const val WRITE_COOP_MISSION = "WRITE_COOP_MISSION"
    const val WRITE_AVATAR = "WRITE_AVATAR"
    const val WRITE_MATCHMAKER_MAP = "WRITE_MATCHMAKER_MAP"
    const val WRITE_EMAIL_DOMAIN_BAN = "WRITE_EMAIL_DOMAIN_BAN"
    const val ADMIN_VOTE = "ADMIN_VOTE"
    const val WRITE_USER_GROUP = "WRITE_USER_GROUP"
    const val READ_USER_GROUP = "READ_USER_GROUP"
    const val WRITE_TUTORIAL = "WRITE_TUTORIAL"
    const val WRITE_NEWS_POST = "WRITE_NEWS_POST"
    const val WRITE_OAUTH_CLIENT = "WRITE_OAUTH_CLIENT"
    const val ADMIN_MAP = "ADMIN_MAP"
    const val ADMIN_MOD = "ADMIN_MOD"
    const val WRITE_MESSAGE = "WRITE_MESSAGE"
}
