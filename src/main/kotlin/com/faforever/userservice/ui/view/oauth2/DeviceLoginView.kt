package com.faforever.userservice.ui.view.oauth2

import com.faforever.userservice.backend.hydra.DeviceAcceptResult
import com.faforever.userservice.backend.hydra.HydraService
import com.faforever.userservice.backend.hydra.NoChallengeException
import com.faforever.userservice.backend.hydra.RedirectTo
import com.faforever.userservice.ui.component.FontAwesomeIcon
import com.faforever.userservice.ui.component.LogoHeader
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.Key
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.progressbar.ProgressBar
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.VaadinSession
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Route("/oauth2/device-login", layout = CardLayout::class)
class DeviceLoginView(
    private val hydraService: HydraService,
) : CompactVerticalLayout(), BeforeEnterObserver {

    private val progressBar = ProgressBar().apply { isIndeterminate = true }

    private val header = LogoHeader().apply { setTitle(getTranslation("device-login.title")) }

    private val instructions = Paragraph(getTranslation("device-login.instructions"))

    private val errorMessage = Span()
    private val errorLayout = HorizontalLayout().apply {
        isVisible = false
        alignItems = FlexComponent.Alignment.CENTER
        setVerticalComponentAlignment(FlexComponent.Alignment.CENTER)
        setWidthFull()
        addClassName("error")
        add(FontAwesomeIcon().apply { addClassNames("fas", "fa-exclamation-triangle") })
        add(errorMessage)
    }

    private val submit = Button(getTranslation("device-login.submit")) { submitUserCode() }.apply {
        setWidthFull()
        isEnabled = false
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        addClickShortcut(Key.ENTER)
    }

    private val userCodeField = TextField(null, getTranslation("device-login.userCode")).apply {
        setWidthFull()
        isAutofocus = true
        valueChangeMode = ValueChangeMode.EAGER
        addClassName("device-user-code")
        element.setAttribute("autocomplete", "off")
        element.setAttribute("autocapitalize", "off")
        element.setAttribute("spellcheck", "false")
        addValueChangeListener {
            clearError()
            submit.isEnabled = normalizeUserCode(it.value).isNotEmpty()
        }
    }

    private val form = CompactVerticalLayout(header, instructions, errorLayout, userCodeField, submit).apply {
        isVisible = false
        setWidthFull()
    }

    private lateinit var challenge: String

    init {
        add(progressBar, form)
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        val params = event.location.queryParameters.parameters
        challenge = params["device_challenge"]?.firstOrNull()?.takeUnless { it.isBlank() }
            ?: throw NoChallengeException()

        // Hydra only forwards the user code when the user opened verification_uri_complete. On the
        // manual / cross device path (RFC 8628 section 3.3) it is absent and we have to ask for it.
        val userCode = normalizeUserCode(params["user_code"]?.firstOrNull())
        if (userCode.isEmpty()) {
            showForm()
            return
        }

        when (val result = hydraService.acceptDeviceRequest(challenge, userCode)) {
            is DeviceAcceptResult.Accepted -> redirect(event.ui, userCode, result.redirectTo)
            DeviceAcceptResult.InvalidUserCode -> {
                showForm()
                userCodeField.value = userCode
                showError(getTranslation("device-login.invalidCode"))
            }

            DeviceAcceptResult.FlowFailed -> {
                showForm()
                showError(getTranslation("device-login.failed"))
                submit.isEnabled = false
            }
        }
    }

    private fun submitUserCode() {
        val userCode = normalizeUserCode(userCodeField.value)
        if (userCode.isEmpty()) {
            showError(getTranslation("device-login.codeRequired"))
            return
        }

        // Guard against a double submit while the Hydra call is in flight.
        submit.isEnabled = false

        val result = try {
            hydraService.acceptDeviceRequest(challenge, userCode)
        } catch (e: RuntimeException) {
            // Exceptions from a click listener never reach the error views, they only trigger the
            // generic internal error notification. Degrade to a readable message instead.
            LOG.error("Accepting the device request failed for challenge {}", challenge, e)
            DeviceAcceptResult.FlowFailed
        }

        when (result) {
            is DeviceAcceptResult.Accepted -> ui.ifPresent { redirect(it, userCode, result.redirectTo) }
            DeviceAcceptResult.InvalidUserCode -> {
                showError(getTranslation("device-login.invalidCode"))
                submit.isEnabled = true
                userCodeField.focus()
            }

            DeviceAcceptResult.FlowFailed -> showError(getTranslation("device-login.failed"))
        }
    }

    private fun redirect(ui: UI, userCode: String, redirectTo: RedirectTo) {
        // Carry the user code across the Hydra round-trip so the login screen can display it. It
        // cannot travel in the URL, Hydra masks the user_code parameter on the redirect hops.
        VaadinSession.getCurrent()?.setAttribute(DEVICE_USER_CODE_SESSION_ATTR, userCode)
        ui.page.setLocation(redirectTo.uri)
    }

    private fun showForm() {
        progressBar.isVisible = false
        form.isVisible = true
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorLayout.isVisible = true
        userCodeField.isInvalid = true
    }

    private fun clearError() {
        errorLayout.isVisible = false
        userCodeField.isInvalid = false
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DeviceLoginView::class.java)

        const val DEVICE_USER_CODE_SESSION_ATTR = "oauth2.deviceUserCode"

        /**
         * Hydra generates user codes from letters and digits only, so everything else is noise the
         * user may have copied along, e.g. surrounding whitespace or a separating dash. The codes
         * are case sensitive, so the case must be preserved.
         */
        fun normalizeUserCode(rawUserCode: String?): String =
            rawUserCode?.filter { it.isLetterOrDigit() }.orEmpty()
    }
}
