package com.smktelkom.mathchat3.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {

    const val LANGUAGE_INDONESIAN = "in"
    const val LANGUAGE_ENGLISH = "en"

    /**
     * Mengatur bahasa aplikasi secara dinamis (Bahasa Indonesia "in" atau English "en")
     */
    fun setLocale(context: Context, languageCode: String) {
        val preferenceManager = PreferenceManager(context.applicationContext)
        preferenceManager.putString("selectedLanguage", languageCode)

        // Gunakan AppCompatDelegate setApplicationLocales untuk Android 13+ dan AndroidX
        val appLocales = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocales)

        // Terapkan ke konfigurasi lokal
        updateResources(context, languageCode)
    }

    fun getSelectedLanguage(context: Context): String {
        val preferenceManager = PreferenceManager(context.applicationContext)
        return preferenceManager.getString("selectedLanguage") ?: LANGUAGE_INDONESIAN
    }

    fun onAttach(context: Context): Context {
        val lang = getSelectedLanguage(context)
        return updateResources(context, lang)
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
