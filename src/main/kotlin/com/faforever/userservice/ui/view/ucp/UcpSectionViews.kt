package com.faforever.userservice.ui.view.ucp

import com.faforever.userservice.ui.layout.UcpLayout
import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll

@Route(value = "/ucp/friends-foes", layout = UcpLayout::class)
@PermitAll
class UcpFriendsFoesView : UcpPlaceholderView("ucp.nav.friendsFoes")

@Route(value = "/ucp/avatars", layout = UcpLayout::class)
@PermitAll
class UcpAvatarsView : UcpPlaceholderView("ucp.nav.avatars")

@Route(value = "/ucp/ban-history", layout = UcpLayout::class)
@PermitAll
class UcpBanHistoryView : UcpPlaceholderView("ucp.nav.banHistory")
