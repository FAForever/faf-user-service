package com.faforever.userservice.ui.component

import com.vaadin.flow.component.AbstractSinglePropertyField
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.dependency.NpmPackage

@NpmPackage(value = "altcha", version = "2.3.0")
@Tag("faf-altcha")
@JsModule("./src/faf-altcha.ts")
class Altcha(challengeUrl: String) : AbstractSinglePropertyField<Altcha, String>("token", "", false) {

    init {
        element.setProperty("challengeUrl", challengeUrl)
    }
}
