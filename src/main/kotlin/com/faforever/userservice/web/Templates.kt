package com.faforever.userservice.web

import io.quarkus.qute.CheckedTemplate
import io.quarkus.qute.TemplateInstance

@CheckedTemplate(basePath="oauth2")
object Templates {

    @JvmStatic
    external fun login(loginData: LoginData): TemplateInstance

    @JvmStatic
    external fun banned(banData: BanData): TemplateInstance

    @JvmStatic
    external fun loginTechnicalError(traceId: String): TemplateInstance

    @JvmStatic
    external fun gameVerificationFailed(accountLink: String): TemplateInstance
}
