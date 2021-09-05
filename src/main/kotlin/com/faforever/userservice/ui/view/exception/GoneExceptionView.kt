package com.faforever.userservice.ui.view.exception

import com.faforever.userservice.backend.hydra.GoneException
import com.faforever.userservice.ui.layout.CardLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.ParentLayout
import jakarta.servlet.http.HttpServletResponse

@Suppress("unused")
@ParentLayout(CardLayout::class)
class GoneExceptionView : ErrorCard(), HasErrorParameter<GoneException> {

    init {
        setTitle(getTranslation("title.technicalError"))
        setMessage(getTranslation("login.technicalError"))
    }

    override fun setErrorParameter(event: BeforeEnterEvent?, parameter: ErrorParameter<GoneException>?): Int {
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }
}
