package com.faforever.userservice.backend.i18n

import com.vaadin.flow.component.UI
import com.vaadin.flow.i18n.I18NProvider
import jakarta.enterprise.context.ApplicationScoped
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.MessageFormat
import java.util.*


@ApplicationScoped
class I18n : I18NProvider {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(I18n::class.java)

        const val BUNDLE_PREFIX = "i18n/messages"
    }

    private val locales: List<Locale> = listOf(Locale.ENGLISH, Locale.GERMAN)

    override fun getProvidedLocales(): List<Locale> {
        return locales
    }

    override fun getTranslation(key: String, locale: Locale, vararg params: Any): String? {
        val bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale)
        var value = try {
            bundle.getString(key)
        } catch (e: MissingResourceException) {
            LOG.warn("Missing resource `$key` for locale `$locale`", e)
            return null
        }
        if (params.isNotEmpty()) {
            value = MessageFormat.format(value, *params)
        }
        return value
    }

    fun getTranslation(key: String, vararg params: Any): String? {
        val locale = UI.getCurrent()?.locale ?: throw IllegalStateException("Cannot retrieve locale from UI. Possibly not in UI thread")
        return getTranslation(key, locale, *params)
    }
}