package com.faforever.userservice.ui.view.registration

import com.faforever.userservice.backend.registration.EmailStatus
import com.faforever.userservice.backend.registration.RegistrationService
import com.faforever.userservice.backend.registration.UsernameStatus
import com.faforever.userservice.config.FafProperties
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
class RegisterView(private val registrationService: RegistrationService, fafProperties: FafProperties) :
    CompactVerticalLayout() {

    companion object {
        class RegistrationInfo {
            var username: String = ""
            var email: String = ""
            var termsOfService: Boolean = false
            var privacyPolicy: Boolean = false
            var rules: Boolean = false
        }
    }

    private val username = TextField(null, getTranslation("register.username")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
    }
    private val email = TextField(null, getTranslation("register.email")).apply {
        setWidthFull()
        valueChangeMode = ValueChangeMode.LAZY
    }
    private val termsOfService = Checkbox(false)
    private val privacyPolicy = Checkbox(false)
    private val rules = Checkbox(false)

    private val submit = Button(getTranslation("register.action")) { register() }.apply {
        isEnabled = false
        addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        setWidthFull()
    }

    private val binder = Binder(RegistrationInfo::class.java)

    init {

        val formHeaderLeft = FafLogo()
        val formHeaderRight = H2(getTranslation("register.action"))
        val formHeader = HorizontalLayout(formHeaderLeft, formHeaderRight).apply {
            justifyContentMode = FlexComponent.JustifyContentMode.CENTER
            alignItems = FlexComponent.Alignment.CENTER
            setId("form-header")
            setWidthFull()
        }

        add(formHeader)

        val readAndAgree = getTranslation("register.readAndAgree") + " "
        val termsOfServiceLayout = CompactHorizontalLayout(
            termsOfService,
            Text(readAndAgree),
            Anchor(
                fafProperties.account().registration().termsOfServiceUrl(),
                getTranslation("register.termsOfService"),
            ).apply { addClassName("policy-link") },
        )
        val privacyPolicyLayout = CompactHorizontalLayout(
            privacyPolicy,
            Text(
                readAndAgree,
            ),
            Anchor(
                fafProperties.account().registration().privacyStatementUrl(),
                getTranslation("register.privacy")
            ).apply { addClassName("policy-link") },
        )
        val rulesLayout = CompactHorizontalLayout(
            rules,
            Text(readAndAgree),
            Anchor(
                fafProperties.account().registration().rulesUrl(),
                getTranslation("register.rules")
            ).apply { addClassName("policy-link") },
        )

        add(username, email, termsOfServiceLayout, privacyPolicyLayout, rulesLayout, submit)

        val footer = VerticalLayout(SocialIcons()).apply {
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
                { email -> registrationService.emailAvailable(email) == EmailStatus.EMAIL_AVAILABLE },
                getTranslation("register.email.taken"),
            ).bind("email")

        binder.forField(termsOfService).asRequired(getTranslation("register.acknowledge.terms")).bind("termsOfService")

        binder.forField(privacyPolicy).asRequired(getTranslation("register.acknowledge.privacy")).bind("privacyPolicy")

        binder.forField(rules).asRequired(getTranslation("register.acknowledge.rules")).bind("rules")

        binder.addStatusChangeListener { submit.isEnabled = it.binder.isValid }
    }

    private fun register() {
        val validationStatus = binder.validate()
        if (validationStatus.hasErrors()) {
            return
        }

        registrationService.register(username.value, email.value)

        val successDialog = Dialog()
        successDialog.add(H2(getTranslation("register.success")))
        successDialog.add(Span(getTranslation("register.success.details", email.value)))
        successDialog.open()

        binder.readBean(null)
    }
}
