package com.faforever.userservice.ui.view.registration

import com.faforever.userservice.backend.account.EmailStatusResponse
import com.faforever.userservice.backend.account.RegistrationService
import com.faforever.userservice.backend.account.UsernameStatus
import com.faforever.userservice.backend.altcha.AltchaService
import com.faforever.userservice.config.FafProperties
import com.faforever.userservice.ui.component.Altcha
import com.faforever.userservice.ui.component.FafLogo
import com.faforever.userservice.ui.component.SocialIcons
import com.faforever.userservice.ui.layout.CardLayout
import com.faforever.userservice.ui.layout.CompactHorizontalLayout
import com.faforever.userservice.ui.layout.CompactVerticalLayout
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.validator.EmailValidator
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.router.Route

@Route("/register", layout = CardLayout::class)
class RegisterView(
    private val registrationService: RegistrationService,
    private val altchaService: AltchaService,
    fafProperties: FafProperties,
) :
    CompactVerticalLayout() {
    class RegistrationInfo {
        var username: String = ""
        var email: String = ""
        var termsOfService: Boolean = false
        var privacyPolicy: Boolean = false
        var rules: Boolean = false
        var recaptcha: String? = null
    }

    private val username =
        TextField(null, getTranslation("register.username")).apply {
            setWidthFull()
            valueChangeMode = ValueChangeMode.LAZY
        }
    private val email =
        TextField(null, getTranslation("register.email")).apply {
            setWidthFull()
            valueChangeMode = ValueChangeMode.LAZY
        }

    private val submit =
        Button(getTranslation("register.action")) { register() }.apply {
            isEnabled = false
            addThemeVariants(ButtonVariant.LUMO_PRIMARY)
            setWidthFull()
        }

    private val binder = Binder(RegistrationInfo::class.java)

    init {

        val termsOfService = Checkbox(false).apply {
            addClassName("policy-checkbox")
        }
        val privacyPolicy = Checkbox(false).apply {
            addClassName("policy-checkbox")
        }
        val rules = Checkbox(false).apply {
            addClassName("policy-checkbox")
        }
        val altcha = Altcha("${fafProperties.selfUrl()}/altcha/challenge")

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("register.title"))
        val formHeader =
            HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
                justifyContentMode = FlexComponent.JustifyContentMode.CENTER
                alignItems = FlexComponent.Alignment.CENTER
                setId("form-header")
                setWidthFull()
            }

        add(formHeader)

        val readAndAgree = getTranslation("register.readAndAgree") + " "
        val termsOfServiceLayout =
            CompactHorizontalLayout(
                termsOfService,
                Div(
                    Text(readAndAgree),
                    Anchor(
                        fafProperties.account().registration().termsOfServiceUrl(),
                        getTranslation("register.termsOfService"),
                    ).apply { addClassName("policy-link") },
                ),
            )
        val privacyPolicyLayout =
            CompactHorizontalLayout(
                privacyPolicy,
                Div(
                    Text(
                        readAndAgree,
                    ),
                    Anchor(
                        fafProperties.account().registration().privacyStatementUrl(),
                        getTranslation("register.privacy"),
                    ).apply { addClassName("policy-link") },
                ),
            )
        val rulesLayout =
            CompactHorizontalLayout(
                rules,
                Div(
                    Text(readAndAgree),
                    Anchor(
                        fafProperties.account().registration().rulesUrl(),
                        getTranslation("register.rules"),
                    ).apply { addClassName("policy-link") },
                ),
            )

        add(username, email, termsOfServiceLayout, privacyPolicyLayout, rulesLayout)

        if (fafProperties.altcha().enabled()) {
            add(altcha)
        }

        add(submit)

        val footer =
            VerticalLayout(SocialIcons()).apply {
                alignItems = FlexComponent.Alignment.CENTER
            }

        add(footer)

        binder.forField(username).asRequired(getTranslation("register.username.required"))
            .withValidator({ username -> username[0].isLetter() }, getTranslation("register.username.startsWithLetter"))
            .withValidator({ username -> username.length in 3..15 }, getTranslation("register.username.size"))
            .withValidator(
                { username -> !Regex("[^A-Za-z0-9_-]").containsMatchIn(username) },
                getTranslation("register.username.alphanumeric"),
            ).withValidator(
                { username -> registrationService.usernameAvailable(username) == UsernameStatus.USERNAME_AVAILABLE },
                getTranslation("register.username.taken"),
            ).bind("username")

        binder.forField(email).withValidator(EmailValidator(getTranslation("register.email.invalid"))).withValidator(
            { email -> registrationService.emailAvailable(email) !is EmailStatusResponse.EmailBlackListed },
            getTranslation("register.email.blacklisted"),
        ).bind("email")

        binder.forField(termsOfService).asRequired(getTranslation("register.acknowledge.terms")).bind("termsOfService")

        binder.forField(privacyPolicy).asRequired(getTranslation("register.acknowledge.privacy")).bind("privacyPolicy")

        binder.forField(rules).asRequired(getTranslation("register.acknowledge.rules")).bind("rules")

        if (fafProperties.altcha().enabled()) {
            binder.forField(altcha).withValidator({ token -> altchaService.verifyPayload(token) }, "")
                .bind("recaptcha")
        }

        binder.addStatusChangeListener { submit.isEnabled = it.binder.isValid }
    }

    private fun register() {
        val validationStatus = binder.validate()
        if (validationStatus.hasErrors()) {
            return
        }

        registrationService.register(username.value, email.value)

        Dialog().apply {
            add(H2(getTranslation("register.success")))
            add(Span(getTranslation("register.success.details", email.value)))
            isCloseOnOutsideClick = false
            open()
        }

        binder.readBean(null)
    }
}
