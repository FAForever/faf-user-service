package com.faforever.userservice.backend.email

import com.faforever.userservice.config.FafProperties
import io.quarkus.runtime.StartupEvent
import jakarta.ejb.Startup
import jakarta.enterprise.event.Observes
import jakarta.inject.Singleton
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

@Startup
@Singleton
class MailBodyBuilder(private val properties: FafProperties) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MailBodyBuilder::class.java)
    }

    enum class Template(vararg variables: String) {
        ACCOUNT_ACTIVATION("username", "activationUrl"),
        WELCOME_TO_FAF("username"),
        PASSWORD_RESET("username", "passwordResetUrl"),
        ;

        val variables: Set<String>

        init {
            this.variables = setOf(*variables)
        }
    }

    private fun getTemplateFilePath(template: Template): Path {
        val path = when (template) {
            Template.ACCOUNT_ACTIVATION -> properties.account().registration().activationMailTemplatePath()
            Template.WELCOME_TO_FAF -> properties.account().registration().welcomeMailTemplatePath()
            Template.PASSWORD_RESET -> properties.account().passwordReset().mailTemplatePath()
        }
        return Path.of(path)
    }

    fun onStart(@Observes event: StartupEvent) {
        var templateError = false
        for (template in Template.values()) {
            val path = getTemplateFilePath(template)
            if (Files.exists(path)) {
                log.debug("Template {} has template file present at {}", template, path)
            } else {
                templateError = true
                log.error("Template {} is missing file at configured destination: {}", template, path)
            }
            try {
                loadAndValidateTemplate(template)
            } catch (e: Exception) {
                log.error("Template {} has invalid template file at {}. Error: {}", template, path, e.message)
                templateError = true
            }
        }
        check(!templateError) { "At least one template file is not available or inconsistent." }
        log.info("All template files present.")
    }

    private fun loadAndValidateTemplate(template: Template): String {
        val templateBody = Files.readString(getTemplateFilePath(template))
        val missingVariables = template.variables
            .map { "{{$it}}" }
            .filterNot { templateBody.contains(it) }
            .joinToString(separator = ", ")
        check(missingVariables.isEmpty()) {
            "Template file for $template is missing variables: $missingVariables"
        }

        return templateBody
    }

    private fun validateVariables(template: Template, variables: Set<String>) {
        val missingVariables = template.variables
            .filterNot { variables.contains(it) }
            .joinToString(separator = ", ")
        val unknownVariables = variables
            .filterNot { template.variables.contains(it) }
            .joinToString(separator = ", ")
        if (unknownVariables.isNotEmpty()) {
            log.warn("Unknown variable(s) handed over for template {}: {}", template, unknownVariables)
        }
        require(missingVariables.isEmpty()) { "Variable(s) not assigned: $missingVariables" }
    }

    private fun populate(template: Template, variables: Map<String, String>): String {
        validateVariables(template, variables.keys)
        var templateBody = loadAndValidateTemplate(template)
        log.trace("Raw template body: {}", templateBody)
        for ((key, value) in variables) {
            val variable = "{{$key}}"
            log.trace("Replacing {} with {}", variable, value)
            templateBody = templateBody.replace(variable, value)
        }
        log.trace("Replaced template body: {}", templateBody)
        return templateBody
    }

    fun buildAccountActivationBody(username: String, activationUrl: String) =
        populate(
            Template.ACCOUNT_ACTIVATION,
            mapOf(
                "username" to username,
                "activationUrl" to activationUrl,
            ),
        )

    fun buildWelcomeToFafBody(username: String) =
        populate(
            Template.WELCOME_TO_FAF,
            mapOf(
                "username" to username,
            ),
        )

    fun buildPasswordResetBody(username: String, passwordResetUrl: String) =
        populate(
            Template.PASSWORD_RESET,
            mapOf(
                "username" to username,
                "passwordResetUrl" to passwordResetUrl,
            ),
        )
}
