package com.faforever.userservice.ui.component

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.AnchorTargetValue
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout

@Tag("i")
open class Icon : Component(), HasComponents {

    fun setAriaLabel(label : String) {
        style.set("aria-label", label)
    }

}

class SocialIcon(link : String, label : String, val type : String) : Anchor(link) {
    init {
        val icon = Icon()
        icon.addClassNames("fab", "fa-${type}")
        icon.setAriaLabel(label)

        add(icon)
        setTarget(AnchorTargetValue.forString("_blank"))
    }
}

class SocialIcons : HorizontalLayout() {
    init {
        justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        add(SocialIcon("https://discord.com/invite/hgvj6Af", "FAF Discord", "discord"))
        add(SocialIcon("https://www.youtube.com/c/ForgedAllianceForever", "FAF Youtube", "youtube"))
        add(SocialIcon("https://www.facebook.com/ForgedAllianceForever", "FAF Facebook", "facebook"))
        add(SocialIcon("https://github.com/FAForever/", "FAF Github", "github"))
        add(SocialIcon("https://www.patreon.com/faf", "FAF Patreon", "patreon"))
    }
}

class InfoTooltipIcon(tooltip: String) : Icon() {
    init {
        addClassNames("tooltip", "fas", "fa-info-circle")

        val tooltipSpan = Span(tooltip)
        tooltipSpan.addClassNames("tooltiptext")
        add(tooltipSpan)
    }
}