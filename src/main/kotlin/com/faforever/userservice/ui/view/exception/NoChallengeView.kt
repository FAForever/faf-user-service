package com.faforever.userservice.ui.view.exception

import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.backend.i18n.I18n
import com.faforever.userservice.ui.component.CompactVerticalLayout
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.layout.CardLayout
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.ParentLayout
import jakarta.servlet.http.HttpServletResponse


@ParentLayout(CardLayout::class)
class NoChallengeView(i18n: I18n) : CompactVerticalLayout(), HasErrorParameter<NoChallengeException> {

    private val errorLayout = HorizontalLayout()
    private val errorMessage = Span()

    init {
        val formHeader = HorizontalLayout()

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(i18n.getTranslation("title.accessDenied"))
        formHeader.add(formHeaderLeft, formHeaderRight)
        formHeader.alignItems = FlexComponent.Alignment.CENTER
        formHeader.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        formHeader.setId("form-header")
        formHeader.setWidthFull()

        add(formHeader)

        errorMessage.text = i18n.getTranslation("login.invalidFlow")

        errorLayout.setWidthFull()
        errorLayout.addClassNames("error", "error-info")
        errorLayout.add(errorMessage)
        errorLayout.alignItems = FlexComponent.Alignment.CENTER
        errorLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)

        add(errorLayout)
    }

    override fun setErrorParameter(event: BeforeEnterEvent?, parameter: ErrorParameter<NoChallengeException>?): Int {
        return HttpServletResponse.SC_FORBIDDEN
    }

}