package com.faforever.userservice.ui.view.exception

import com.faforever.userservice.backend.registration.InvalidRegistrationException
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
class InvalidRegistrationExceptionView :
    CompactVerticalLayout(),
    HasErrorParameter<InvalidRegistrationException> {
    private val errorMessage =
        Span().apply {
            text = getTranslation("register.technicalError")
        }
    private val errorLayout =
        HorizontalLayout(errorMessage).apply {
            alignItems = FlexComponent.Alignment.CENTER
            setWidthFull()
            addClassNames("error", "error-info")
            setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)
        }

    init {
        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("title.technicalError"))

        val formHeader =
            HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
                alignItems = FlexComponent.Alignment.CENTER
                justifyContentMode = FlexComponent.JustifyContentMode.CENTER
                setWidthFull()
                setId("form-header")
            }

        add(formHeader)
        add(errorLayout)
    }

    override fun setErrorParameter(
        event: BeforeEnterEvent?,
        parameter: ErrorParameter<InvalidRegistrationException>?,
    ): Int = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
}
