package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.router.Route

@Route(value = "/ucp", layout = UcpLayout::class)
class UcpAccountDataView : UcpPlaceholderView("ucp.nav.accountData")

@Route(value = "/ucp/username", layout = UcpLayout::class)
class UcpChangeUsernameView : UcpPlaceholderView("ucp.nav.changeUsername")

@Route(value = "/ucp/email", layout = UcpLayout::class)
class UcpChangeEmailView : UcpPlaceholderView("ucp.nav.changeEmail")

@Route(value = "/ucp/password", layout = UcpLayout::class)
class UcpChangePasswordView : UcpPlaceholderView("ucp.nav.changePassword")

@Route(value = "/ucp/linking", layout = UcpLayout::class)
class UcpAccountLinkingView : UcpPlaceholderView("ucp.nav.accountLinking")

@Route(value = "/ucp/friends-foes", layout = UcpLayout::class)
class UcpFriendsFoesView : UcpPlaceholderView("ucp.nav.friendsFoes")

@Route(value = "/ucp/avatars", layout = UcpLayout::class)
class UcpAvatarsView : UcpPlaceholderView("ucp.nav.avatars")

@Route(value = "/ucp/ban-history", layout = UcpLayout::class)
class UcpBanHistoryView : UcpPlaceholderView("ucp.nav.banHistory")

@Route(value = "/ucp/delete-account", layout = UcpLayout::class)
class UcpDeleteAccountView : UcpPlaceholderView("ucp.nav.deleteAccount")
