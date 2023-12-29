package com.faforever.userservice.ui.component

import com.vaadin.flow.component.AbstractSinglePropertyField
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule

@Tag("faf-recaptcha")
@JsModule("./src/faf-recaptcha.ts")
class ReCaptcha(siteKey: String?) : AbstractSinglePropertyField<ReCaptcha, String>("token", "", false) {

    init {
        element.setProperty("siteKey", siteKey)
    }
}
