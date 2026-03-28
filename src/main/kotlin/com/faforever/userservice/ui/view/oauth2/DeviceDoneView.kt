package com.faforever.userservice.ui.view.oauth2

import com.faforever.userservice.ui.component.LogoHeader
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.Route

@Route("/oauth2/device-done", layout = CardLayout::class)
class DeviceDoneView : CompactVerticalLayout() {

    init {
        add(
            LogoHeader().apply { setTitle(getTranslation("device-done.title")) },
            Span(getTranslation("device-done.message")),
        )
    }
}
