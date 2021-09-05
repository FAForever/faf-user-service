package com.faforever.userservice.ui.view.exception

import com.faforever.userservice.backend.hydra.GoneException
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.ParentLayout
import jakarta.servlet.http.HttpServletResponse

@Suppress("unused")
@ParentLayout(CardLayout::class)
class GoneExceptionView : CompactVerticalLayout(), HasErrorParameter<GoneException> {

    private val errorLayout = HorizontalLayout()
    private val errorMessage = Span()

    init {
        val formHeader = HorizontalLayout()

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("title.technicalError"))
        formHeader.add(formHeaderLeft, formHeaderRight)
        formHeader.alignItems = FlexComponent.Alignment.CENTER
        formHeader.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        formHeader.setId("form-header")
        formHeader.setWidthFull()

        add(formHeader)

        errorMessage.text = getTranslation("login.technicalError")

        errorLayout.setWidthFull()
        errorLayout.addClassNames("error", "error-info")
        errorLayout.add(errorMessage)
        errorLayout.alignItems = FlexComponent.Alignment.CENTER
        errorLayout.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)

        add(errorLayout)
    }

    override fun setErrorParameter(event: BeforeEnterEvent?, parameter: ErrorParameter<GoneException>?): Int {
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    }

}