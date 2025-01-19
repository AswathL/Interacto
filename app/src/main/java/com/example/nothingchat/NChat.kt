package com.example.nothingchat

import android.app.Application
import com.example.nothingchat.feature.auth.signup.updateLocale
import com.example.nothingchat.utils.PreferencesUtils.getSelectedLanguage

import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class NChat:Application() {
    override fun onCreate() {
        super.onCreate()
        val savedLanguage = getSelectedLanguage(this)
        val locale = Locale(savedLanguage ?: "en")
        updateLocale(this, locale)
    }

}