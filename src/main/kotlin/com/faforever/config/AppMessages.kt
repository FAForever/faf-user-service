package com.faforever.config

import io.quarkus.qute.i18n.Message
import io.quarkus.qute.i18n.MessageBundle

@MessageBundle
interface AppMessages {

    @Message fun header_loggedInAs(name: String): String
    @Message fun title(): String
    @Message fun title_technicalError(): String
    @Message fun login(): String
    @Message fun login_title(): String
    @Message fun login_welcomeBack(): String
    @Message fun login_badCredentials(): String
    @Message fun login_throttled(): String
    @Message fun login_usernameOrEmail(): String
    @Message fun login_password(): String
    @Message fun login_loginAction(): String
    @Message fun login_forgotPassword(): String
    @Message fun login_registerAccount(): String
    @Message fun login_technicalError(): String
    @Message fun consent_appRequest(): String
    @Message fun consent_termsOfService(): String
    @Message fun consent_privacyStatement(): String
    @Message fun consent_authorize(): String
    @Message fun consent_deny(): String
    @Message fun consent_clientLogo(): String
    @Message fun steam_faq(): String
    @Message fun steam_reason(): String
    @Message fun verification_title(): String
    @Message fun oauth2_scope_textMissing(): String
    @Message fun oauth2_scope_openid(): String
    @Message fun oauth2_scope_openid_description(): String
    @Message fun oauth2_scope_offline(): String
    @Message fun oauth2_scope_offline_description(): String
    @Message fun oauth2_scope_public_profile(): String
    @Message fun oauth2_scope_public_profile_description(): String
    @Message fun oauth2_scope_write_account_data(): String
    @Message fun oauth2_scope_write_account_data_description(): String
    @Message fun oauth2_scope_edit_clan_data(): String
    @Message fun oauth2_scope_edit_clan_data_description(): String
    @Message fun oauth2_scope_vote(): String
    @Message fun oauth2_scope_upload_map(): String
    @Message fun oauth2_scope_upload_mod(): String
    @Message fun oauth2_scope_manage_vault(): String
    @Message fun oauth2_scope_manage_vault_description(): String
    @Message fun oauth2_scope_administrative_actions(): String
    @Message fun oauth2_scope_administrative_actions_description(): String
    @Message fun oauth2_scope_read_sensible_userdata(): String
    @Message fun oauth2_scope_read_sensible_userdata_description(): String
    @Message fun oauth2_scope_upload_avatar(): String
    @Message fun oauth2_scope_lobby(): String
    @Message fun ban_title(): String
    @Message fun ban_expiration(): String
    @Message fun ban_permanent(): String
    @Message fun ban_reason(): String
    @Message fun ban_appeal(): String
}