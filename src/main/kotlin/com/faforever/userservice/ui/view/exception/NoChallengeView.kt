package com.faforever.userservice.ui.view.exception

import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.ui.component.ErrorCard
import com.faforever.userservice.ui.layout.CardLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.ParentLayout
import jakarta.servlet.http.HttpServletResponse

@Suppress("unused")
@ParentLayout(CardLayout::class)
class NoChallengeView : ErrorCard(), HasErrorParameter<NoChallengeException> {

    init {
        setTitle(getTranslation("title.accessDenied"))
        setMessage(getTranslation("login.invalidFlow"))
    }

    override fun setErrorParameter(event: BeforeEnterEvent?, parameter: ErrorParameter<NoChallengeException>?): Int {
        return HttpServletResponse.SC_FORBIDDEN
    }
}
