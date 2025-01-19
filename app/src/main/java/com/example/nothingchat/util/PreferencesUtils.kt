package com.example.nothingchat.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object PreferencesUtils {

    private const val PREF_NAME = "AppPreferences"
    private const val KEY_SELECTED_LANGUAGE = "SelectedLanguage"

    fun saveSelectedLanguage(context: Context, languageCode: String) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_SELECTED_LANGUAGE, languageCode)
        editor.apply()
    }

    fun getSelectedLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_SELECTED_LANGUAGE, "en") ?: "en" // Default to English
    }

    fun applyLanguage(context: Context): Context {
        val languageCode = getSelectedLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
