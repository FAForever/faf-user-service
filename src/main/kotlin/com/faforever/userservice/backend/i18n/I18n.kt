package com.faforever.userservice.backend.i18n

import com.vaadin.flow.i18n.I18NProvider
import com.vaadin.quarkus.annotation.VaadinServiceEnabled
import io.quarkus.arc.Unremovable
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Default
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.*

@ApplicationScoped
@VaadinServiceEnabled
@Unremovable
@Default
class I18n : I18NProvider {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(I18n::class.java)

        const val BUNDLE_PREFIX = "i18n/messages"
    }

    override fun getProvidedLocales(): List<Locale> {
        return listOf(Locale.ENGLISH, Locale.GERMAN)
    }

    override fun getTranslation(key: String, locale: Locale, vararg params: Any): String? {
        val bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale)
        var value = try {
            bundle.getString(key)
        } catch (e: MissingResourceException) {
            LOG.warn("Missing resource `$key` for locale `$locale`", e)
            return "!{$key}!"
        }
        if (params.isNotEmpty()) {
            value = MessageFormat.format(value, *params)
        }
        return value
    }
}