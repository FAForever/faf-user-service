package com.faforever.userservice.ui.view.registration

import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.HtmlComponent
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.FlexLayout
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.Route

@Route("/recover-account", layout = CardLayout::class)
class RecoverAccountView :
    CompactVerticalLayout() {

    private val emailSection = VerticalLayout(
        Button(getTranslation("recovery.selectMethod.email.link")) {
            UI.getCurrent().navigate("/recover-account/email")
        }.apply {
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        },
        HtmlComponent("small").apply {
            element.text = (getTranslation("recovery.selectMethod.email.description"))
        },
    ).apply {
        maxWidth = "50%"
    }

    private val steamSection = VerticalLayout(
        Button(getTranslation("recovery.selectMethod.steam.link")) {
            UI.getCurrent().navigate("/recover-account/steam")
        }.apply {
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        },
        HtmlComponent("small").apply {
            element.text = (getTranslation("recovery.selectMethod.steam.description"))
        },
    ).apply {
        maxWidth = "50%"
    }

    init {
        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("recovery.selectMethod.title"))

        val formHeader = HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
            justifyContentMode = FlexComponent.JustifyContentMode.CENTER
            alignItems = FlexComponent.Alignment.CENTER
            setId("form-header")
            setWidthFull()
        }

        add(formHeader)
        add(
            FlexLayout(emailSection, steamSection).apply {
                flexWrap = FlexLayout.FlexWrap.WRAP
            },
        )

        val footer = VerticalLayout(SocialIcons()).apply {
            alignItems = FlexComponent.Alignment.CENTER
        }

        add(footer)
    }
}
