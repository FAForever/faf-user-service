package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/username", layout = UcpLayout::class)
@PermitAll
class UcpChangeUsernameView : UcpPlaceholderView("ucp.nav.changeUsername")

@Route(value = "/ucp/email", layout = UcpLayout::class)
@PermitAll
class UcpChangeEmailView : UcpPlaceholderView("ucp.nav.changeEmail")

@Route(value = "/ucp/password", layout = UcpLayout::class)
@PermitAll
class UcpChangePasswordView : UcpPlaceholderView("ucp.nav.changePassword")

@Route(value = "/ucp/linking", layout = UcpLayout::class)
@PermitAll
class UcpAccountLinkingView : UcpPlaceholderView("ucp.nav.accountLinking")

@Route(value = "/ucp/friends-foes", layout = UcpLayout::class)
@PermitAll
class UcpFriendsFoesView : UcpPlaceholderView("ucp.nav.friendsFoes")

@Route(value = "/ucp/avatars", layout = UcpLayout::class)
@PermitAll
class UcpAvatarsView : UcpPlaceholderView("ucp.nav.avatars")

@Route(value = "/ucp/ban-history", layout = UcpLayout::class)
@PermitAll
class UcpBanHistoryView : UcpPlaceholderView("ucp.nav.banHistory")

@Route(value = "/ucp/delete-account", layout = UcpLayout::class)
@PermitAll
class UcpDeleteAccountView : UcpPlaceholderView("ucp.nav.deleteAccount")
