package com.faforever.web

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance

@CheckedTemplate(basePath="oauth2")
object Templates {

    @JvmStatic
    external fun loginView(loginData: LoginData): TemplateInstance
}
